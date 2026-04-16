package com.bodyswitch.checkin.ui.checkin

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bodyswitch.checkin.data.api.KioskApi
import com.bodyswitch.checkin.data.api.dto.AttendRequest
import com.bodyswitch.checkin.data.api.dto.CheckinRequest
import com.bodyswitch.checkin.data.api.dto.ErrorResponse
import com.bodyswitch.checkin.data.session.EmployeeLoginHolder
import com.bodyswitch.checkin.data.api.dto.QrLoginRequest
import com.bodyswitch.checkin.data.model.CoursePass
import com.bodyswitch.checkin.data.model.Member
import com.bodyswitch.checkin.data.model.Reservation
import com.bodyswitch.checkin.data.model.Ticket
import com.bodyswitch.checkin.data.model.TicketType
import com.bodyswitch.checkin.data.network.NetworkMonitor
import com.bodyswitch.checkin.data.session.SessionManager
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

data class CheckinUiState(
    val isLoading: Boolean = true,
    val member: Member? = null,
    val selectedTicketId: Long? = null,
    val selectedTicketType: TicketType? = null,
    val deductCount: Int = 1,
    val checkinDone: Boolean = false,
    val autoCheckinDone: Boolean = false,
    val checkinMessage: String? = null,
    val error: String? = null,
    // 예약 관련
    val reservationsLoading: Boolean = false,
    val reservations: List<Reservation> = emptyList(),
    val reservationsLoaded: Boolean = false,
    val noReservations: Boolean = false,
    val selectedReservationId: Long? = null,
    // 직원 → 선택 화면으로 이동
    val isEmployee: Boolean = false,
)

@HiltViewModel
class CheckinViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val api: KioskApi,
    private val moshi: Moshi,
    private val sessionManager: SessionManager,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val qrData: String? = savedStateHandle.get<String>("qrData")?.let {
        URLDecoder.decode(it, "UTF-8")
    }
    private val passedToken: String? = savedStateHandle.get<String>("token")?.let {
        URLDecoder.decode(it, "UTF-8")
    }

    private val _uiState = MutableStateFlow(CheckinUiState())
    val uiState: StateFlow<CheckinUiState> = _uiState.asStateFlow()

    private var token: String? = passedToken
    private val checkInMethod: String = if (qrData != null) "QR" else "PHONE"

    init {
        if (passedToken != null) {
            viewModelScope.launch { loadTickets() }
        } else if (qrData != null) {
            authenticate()
        } else {
            _uiState.value = CheckinUiState(
                isLoading = false,
                error = "인증 정보가 없습니다",
            )
        }
    }

    private fun authenticate() {
        viewModelScope.launch {
            _uiState.value = CheckinUiState(isLoading = true)

            try {
                Log.d("CHECKIN", "QR 로그인 시도: $qrData")
                val loginResponse = api.qrLogin(
                    adminToken = sessionManager.token,
                    request = QrLoginRequest(qrPayload = qrData ?: ""),
                )
                token = loginResponse.token
                Log.d("CHECKIN", "로그인 성공: ${loginResponse.name}, role=${loginResponse.role}")

                if (loginResponse.role == "EMPLOYEE") {
                    EmployeeLoginHolder.set(
                        token = token ?: return@launch,
                        branchId = loginResponse.branchId ?: sessionManager.branchId ?: return@launch,
                        employeeName = loginResponse.name,
                        checkInMethod = checkInMethod,
                    )
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isEmployee = true,
                    )
                    return@launch
                }

                loadTickets()
            } catch (e: retrofit2.HttpException) {
                val errorMsg = when (e.code()) {
                    404 -> "회원을 찾을 수 없습니다"
                    401 -> "QR 코드가 유효하지 않습니다"
                    406 -> "해당 지점의 회원이 아닙니다"
                    else -> "로그인 실패 (${e.code()})"
                }
                Log.e("CHECKIN", "로그인 실패", e)
                _uiState.value = _uiState.value.copy(isLoading = false, error = errorMsg)
            } catch (e: Exception) {
                Log.e("CHECKIN", "네트워크 오류", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = networkMonitor.networkErrorMessage(),
                )
            }
        }
    }

    private suspend fun loadTickets() {
        val bearerToken = "Bearer ${token ?: return}"

        try {
            val response = api.getTickets(bearerToken, branchId = sessionManager.branchId)
            Log.d("CHECKIN", "이용권 조회 성공: ${response.memberName}")

            val tickets = mutableListOf<Ticket>()

            response.courseTickets?.forEach { dto ->
                tickets.add(
                    Ticket(
                        id = dto.id,
                        name = dto.ticketName,
                        type = TicketType.COURSE_TICKET,
                        usageCount = dto.usageCount,
                        remainCount = dto.remainCount,
                        startDate = dto.startDate,
                        expireDate = dto.expireDate,
                        status = dto.status,
                    )
                )
            }

            response.trialTickets?.forEach { dto ->
                tickets.add(
                    Ticket(
                        id = dto.id,
                        name = dto.ticketName,
                        type = TicketType.TRIAL_TICKET,
                        usageCount = dto.usageCount,
                        remainCount = dto.remainCount,
                        startDate = dto.startDate,
                        expireDate = dto.expireDate,
                        status = dto.status,
                    )
                )
            }

            val passes = response.coursePasses?.map { dto ->
                CoursePass(
                    id = dto.id,
                    name = dto.passName,
                    startDate = dto.startDate,
                    expireDate = dto.expireDate,
                    status = dto.status,
                )
            } ?: emptyList()

            val member = Member(
                id = response.memberId ?: "",
                name = response.memberName,
                tickets = tickets,
                passes = passes,
            )

            val activeTickets = tickets.filter { it.status != "INACTIVE" }
            val activePasses = passes.filter { it.status != "INACTIVE" }

            // 이용권(기간권)만 있으면 자동 체크인
            if (activeTickets.isEmpty() && activePasses.isNotEmpty()) {
                val pass = activePasses.first()
                _uiState.value = CheckinUiState(
                    isLoading = true,
                    member = member,
                    selectedTicketId = pass.id,
                    selectedTicketType = TicketType.COURSE_PASS,
                    deductCount = 0,
                )
                performCheckin(isAuto = true)
                return
            }

            _uiState.value = CheckinUiState(
                isLoading = false,
                member = member,
            )
        } catch (e: Exception) {
            Log.e("CHECKIN", "이용권 조회 실패", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "이용권 정보를 불러올 수 없습니다",
            )
        }
    }

    fun selectTicket(ticketId: Long, ticketType: TicketType) {
        val prev = _uiState.value
        // 같은 티켓 재선택 시 무시
        if (prev.selectedTicketId == ticketId && prev.selectedTicketType == ticketType) return

        _uiState.value = prev.copy(
            selectedTicketId = ticketId,
            selectedTicketType = ticketType,
            deductCount = if (ticketType == TicketType.COURSE_PASS) 0 else 1,
            // 예약 상태 초기화
            reservations = emptyList(),
            reservationsLoaded = false,
            noReservations = false,
            selectedReservationId = null,
        )

        // 수강권/체험권 선택 시 예약 조회
        if (ticketType == TicketType.COURSE_TICKET || ticketType == TicketType.TRIAL_TICKET) {
            loadReservations(ticketId, ticketType)
        }
    }

    private fun loadReservations(ticketId: Long, ticketType: TicketType) {
        viewModelScope.launch {
            val bearerToken = "Bearer ${token ?: return@launch}"
            _uiState.value = _uiState.value.copy(reservationsLoading = true)

            try {
                val response = api.getReservations(
                    authorization = bearerToken,
                    branchId = sessionManager.branchId ?: return@launch,
                    ticketType = ticketType.apiValue,
                    ticketId = ticketId,
                )
                val reservations = response.reservations.map { dto ->
                    Reservation(
                        reservationId = dto.reservationId,
                        courseClassName = dto.courseClassName,
                        classDate = dto.classDate,
                        startTime = dto.startTime,
                        endTime = dto.endTime,
                        roomName = dto.roomName,
                        employeeName = dto.employeeName,
                        classType = dto.classType,
                        status = dto.status,
                        ticketName = dto.ticketName,
                    )
                }
                _uiState.value = _uiState.value.copy(
                    reservationsLoading = false,
                    reservations = reservations,
                    reservationsLoaded = true,
                    noReservations = reservations.isEmpty(),
                )
                Log.d("CHECKIN", "예약 조회 성공: ${reservations.size}건")
            } catch (e: Exception) {
                Log.e("CHECKIN", "예약 조회 실패", e)
                _uiState.value = _uiState.value.copy(
                    reservationsLoading = false,
                    reservationsLoaded = true,
                    noReservations = true,
                )
            }
        }
    }

    fun selectReservation(reservationId: Long) {
        _uiState.value = _uiState.value.copy(selectedReservationId = reservationId)
    }

    fun setDeductCount(count: Int) {
        _uiState.value = _uiState.value.copy(deductCount = count.coerceAtLeast(1))
    }

    fun checkin() {
        viewModelScope.launch {
            val state = _uiState.value
            when {
                // 예약이 선택된 경우 출석 처리
                state.selectedReservationId != null -> performAttend()
                // 이용권(기간권)은 예약 없이 체크인 가능
                state.selectedTicketType == TicketType.COURSE_PASS -> performCheckin(isAuto = false)
                // 수강권/체험권은 예약 선택 필수 → 방어적으로 무시
                else -> return@launch
            }
        }
    }

    private suspend fun performAttend() {
        val state = _uiState.value
        val reservationId = state.selectedReservationId ?: return
        val bearerToken = "Bearer ${token ?: return}"

        _uiState.value = state.copy(isLoading = true)

        try {
            val response = api.attend(
                authorization = bearerToken,
                adminToken = sessionManager.token,
                request = AttendRequest(
                    branchId = sessionManager.branchId ?: return,
                    reservationId = reservationId,
                    checkInMethod = checkInMethod,
                ),
            )
            Log.d("CHECKIN", "출석 처리 성공: ${response.message}")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                checkinDone = true,
                checkinMessage = response.message,
            )
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            val errorMsg = try {
                moshi.adapter(ErrorResponse::class.java)
                    .fromJson(errorBody ?: "")?.message
            } catch (_: Exception) {
                null
            } ?: "출석 처리에 실패했습니다 (${e.code()})"
            Log.e("CHECKIN", "출석 처리 실패: $errorMsg")
            _uiState.value = _uiState.value.copy(isLoading = false, error = errorMsg)
        } catch (e: Exception) {
            Log.e("CHECKIN", "출석 네트워크 오류", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = networkMonitor.networkErrorMessage(),
            )
        }
    }

    private suspend fun performCheckin(isAuto: Boolean) {
        val state = _uiState.value
        val ticketId = state.selectedTicketId ?: return
        val ticketType = state.selectedTicketType ?: return
        val bearerToken = "Bearer ${token ?: return}"

        _uiState.value = state.copy(isLoading = true)

        try {
            val response = api.checkin(
                authorization = bearerToken,
                request = CheckinRequest(
                    ticketType = ticketType.apiValue,
                    ticketId = ticketId,
                    deductCount = state.deductCount,
                    checkInMethod = checkInMethod,
                    branchId = sessionManager.branchId,
                ),
            )

            if (response.isSuccessful) {
                val body = response.body()
                Log.d("CHECKIN", "체크인 성공: ${body?.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    checkinDone = !isAuto,
                    autoCheckinDone = isAuto,
                    checkinMessage = body?.message,
                )
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = try {
                    moshi.adapter(ErrorResponse::class.java)
                        .fromJson(errorBody ?: "")?.message
                } catch (_: Exception) {
                    null
                } ?: "체크인에 실패했습니다 (${response.code()})"

                Log.e("CHECKIN", "체크인 실패: $errorMsg")
                _uiState.value = _uiState.value.copy(isLoading = false, error = errorMsg)
            }
        } catch (e: Exception) {
            Log.e("CHECKIN", "체크인 네트워크 오류", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = networkMonitor.networkErrorMessage(),
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
