package com.bodyswitch.checkin.ui.checkin

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bodyswitch.checkin.data.api.KioskApi
import com.bodyswitch.checkin.data.api.dto.AttendRequest
import com.bodyswitch.checkin.data.api.dto.CheckinRequest
import com.bodyswitch.checkin.data.api.dto.CheckoutRequest
import com.bodyswitch.checkin.data.api.dto.ErrorResponse
import com.bodyswitch.checkin.data.api.dto.OpenDoorRequest
import com.bodyswitch.checkin.data.api.dto.ReentryRequest
import com.bodyswitch.checkin.data.session.CheckinSettingsManager
import com.bodyswitch.checkin.data.session.EmployeeLoginHolder
import com.bodyswitch.checkin.data.api.dto.QrLoginRequest
import com.bodyswitch.checkin.data.model.CoursePass
import com.bodyswitch.checkin.data.model.Member
import com.bodyswitch.checkin.data.model.Reservation
import com.bodyswitch.checkin.data.model.Ticket
import com.bodyswitch.checkin.data.model.TicketType
import com.bodyswitch.checkin.data.network.CheckinFailure
import com.bodyswitch.checkin.data.network.CheckinFailureReporter
import com.bodyswitch.checkin.data.network.NetworkMonitor
import com.bodyswitch.checkin.data.network.toCheckinFailure
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
    // 선택된 티켓이 이용권형(이용권 또는 PASS형 체험권)인지 — true면 예약/차감 없이 바로 체크인
    val selectedTicketIsPass: Boolean = false,
    val deductCount: Int = 1,
    val checkinDone: Boolean = false,
    val checkinMessage: String? = null,
    val error: String? = null,
    // 예약 관련
    val reservationsLoading: Boolean = false,
    val reservations: List<Reservation> = emptyList(),
    val reservationsLoaded: Boolean = false,
    val noReservations: Boolean = false,
    val selectedReservationId: Long? = null,
    // 당일 미마감 입장 기록이 있는 회원 → [계속 이용|재입장]/[퇴실] 선택 대기
    val needsAttendChoice: Boolean = false,
    // 무차감 재입장 자격. 없으면 "계속 이용"이 기존 이용권 선택 흐름으로 간다.
    val canReentry: Boolean = false,
    val reentryMessage: String? = null,
    val checkoutDone: Boolean = false,
    // 직원 → 선택 화면으로 이동
    val isEmployee: Boolean = false,
    // 지점 로그인 안 됨(branchId 없음) → 로그인 화면으로
    val requireLogin: Boolean = false,
)

@HiltViewModel
class CheckinViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val api: KioskApi,
    private val moshi: Moshi,
    private val sessionManager: SessionManager,
    private val networkMonitor: NetworkMonitor,
    private val settingsManager: CheckinSettingsManager,
    private val failureReporter: CheckinFailureReporter,
) : ViewModel() {

    private val qrData: String? = savedStateHandle.get<String>("qrData")?.let {
        URLDecoder.decode(it, "UTF-8")
    }
    private val passedToken: String? = savedStateHandle.get<String>("token")?.let {
        URLDecoder.decode(it, "UTF-8")
    }

    private val _uiState = MutableStateFlow(CheckinUiState())
    val uiState: StateFlow<CheckinUiState> = _uiState.asStateFlow()

    /** 만료 이용권 숨김 여부(설정 화면에서 저장하는 기기 로컬 값). */
    val hideExpiredTickets: Boolean = settingsManager.hideExpiredTicketsEnabled

    /** 빈 목록 안내 문구를 "예약 없음" 쪽으로 바꾸기 위한 설정값. */
    val todayOnlyTickets: Boolean = settingsManager.todayOnlyTicketsEnabled

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
            val startedAt = SystemClock.elapsedRealtime()

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
            } catch (e: Exception) {
                val failure = e.classify("로그인에 실패했습니다")
                val errorMsg = if (e is retrofit2.HttpException) {
                    when (e.code()) {
                        404 -> "회원을 찾을 수 없습니다"
                        401 -> "QR 코드가 유효하지 않습니다"
                        406 -> "해당 지점의 회원이 아닙니다"
                        else -> "로그인 실패 (${e.code()})"
                    }
                } else {
                    failure.userMessage
                }
                Log.e("CHECKIN", "로그인 실패", e)
                reportFailure(CheckinFailureReporter.Step.QR_LOGIN, failure, startedAt)
                _uiState.value = _uiState.value.copy(isLoading = false, error = errorMsg)
            }
        }
    }

    private suspend fun loadTickets() {
        // 지점 로그인이 안 된 상태(branchId 없음)면 요청을 보내지 않고 로그인 화면으로 유도한다.
        if (sessionManager.branchId == null) {
            Log.w("CHECKIN", "지점 정보 없음(branchId null) - 로그인 필요")
            _uiState.value = _uiState.value.copy(isLoading = false, requireLogin = true)
            return
        }
        val bearerToken = "Bearer ${token ?: return}"
        val startedAt = SystemClock.elapsedRealtime()

        try {
            val response = api.getTickets(
                bearerToken,
                branchId = sessionManager.branchId,
                todayOnly = settingsManager.todayOnlyTicketsEnabled,
            )
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
                        classType = dto.classType,
                        usageType = dto.usageType,
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

            // 설정에서 "재입장 시 이용권 다시 확인"을 켜면 무차감 재입장을 쓰지 않고
            // 일반 이용권 선택 흐름으로 보낸다. canReentry는 자동 재입장과
            // 선택 화면의 "계속 이용" 양쪽에서 쓰이므로 여기 한 곳만 막으면 된다.
            val canReentry = response.reentry?.eligible == true
                && !settingsManager.recheckTicketOnReentry

            // 당일 미마감 입장 기록이 있으면 계속 이용할지 퇴실할지 묻는다.
            // 재입장 자격(reentry)으로 묻지 않는 이유는 그 판정이 키오스크 입장과
            // "수업 종료 전" 레슨만 보기 때문이다. 안면인식 입장자와 수업이 끝난 회원,
            // 즉 실제로 나가려는 회원이 그 조건에서 빠진다.
            if (response.checkoutAvailable) {
                _uiState.value = CheckinUiState(
                    isLoading = false,
                    member = member,
                    needsAttendChoice = true,
                    canReentry = canReentry,
                    reentryMessage = response.reentry?.message,
                )
                return
            }

            // 미마감 기록이 없는데 재입장 자격만 있는 경우(출석 행 누락 등)는 기존대로 자동 재입장
            if (canReentry) {
                _uiState.value = CheckinUiState(isLoading = true, member = member)
                performReentry(response.reentry?.message)
                return
            }

            // ── [비활성] 이용권 전용 회원 자동 체크인 ──────────────────────────────
            // 이용권만 가진 회원을 선택 화면 없이 바로 체크인시키던 분기. 요청으로 꺼뒀고
            // 되살릴 수 있게 남긴다. 주석을 풀면 그대로 동작한다.
            //
            // 껐던 이유와, 되살릴 때 같이 볼 것:
            //   1) activeTickets 에 일일권도 들어간다. 유효 일일권이 한 장이라도 있으면
            //      이 분기에 걸리지 않는다.
            //   2) 이용권이 여러 개면 first() 로 임의 선택한다. 서버가 정렬해서 주지 않는다.
            //   3) 만료일이 아닌 status 만 본다. 만료 배치가 놓쳐 ACTIVE 로 남은 이용권도 통과한다.
            //   4) 예전에는 autoCheckinDone 으로 완료 화면을 건너뛰어 회원이 성공 확인을
            //      못 봤다. 지금 풀면 performCheckin() 이 checkinDone 을 세워 완료 화면까지 간다.
            //
            // val activeTickets = tickets.filter { it.status != "INACTIVE" }
            // val activePasses = passes.filter { it.status != "INACTIVE" }
            // if (activeTickets.isEmpty() && activePasses.isNotEmpty()) {
            //     val pass = activePasses.first()
            //     _uiState.value = CheckinUiState(
            //         isLoading = true,
            //         member = member,
            //         selectedTicketId = pass.id,
            //         selectedTicketType = TicketType.COURSE_PASS,
            //         deductCount = 0,
            //     )
            //     performCheckin()
            //     return
            // }
            // ──────────────────────────────────────────────────────────────────

            _uiState.value = CheckinUiState(
                isLoading = false,
                member = member,
            )
        } catch (e: Exception) {
            val failure = e.classify("이용권 정보를 불러올 수 없습니다")
            Log.e("CHECKIN", "이용권 조회 실패", e)
            reportFailure(CheckinFailureReporter.Step.TICKETS, failure, startedAt)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = failure.userMessage,
            )
        }
    }

    fun selectTicket(ticketId: Long, ticketType: TicketType) {
        val prev = _uiState.value
        // 같은 티켓 재선택 시 무시
        if (prev.selectedTicketId == ticketId && prev.selectedTicketType == ticketType) return

        // PASS형 체험권은 이용권처럼 예약/차감 없이 바로 입장
        val isPassTypeTrial = ticketType == TicketType.TRIAL_TICKET &&
            prev.member?.tickets
                ?.firstOrNull { it.id == ticketId && it.type == TicketType.TRIAL_TICKET }
                ?.isPassType == true
        val isPassLike = ticketType == TicketType.COURSE_PASS || isPassTypeTrial

        _uiState.value = prev.copy(
            selectedTicketId = ticketId,
            selectedTicketType = ticketType,
            selectedTicketIsPass = isPassLike,
            deductCount = if (isPassLike) 0 else 1,
            // 예약 상태 초기화
            reservations = emptyList(),
            reservationsLoaded = false,
            noReservations = false,
            selectedReservationId = null,
        )

        // 예약 차감형 수강권/체험권만 예약 조회 (PASS형 체험권 제외)
        if (!isPassLike && (ticketType == TicketType.COURSE_TICKET || ticketType == TicketType.TRIAL_TICKET)) {
            loadReservations(ticketId, ticketType)
        }
    }

    private fun loadReservations(ticketId: Long, ticketType: TicketType) {
        viewModelScope.launch {
            val bearerToken = "Bearer ${token ?: return@launch}"
            _uiState.value = _uiState.value.copy(reservationsLoading = true)
            val startedAt = SystemClock.elapsedRealtime()

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
                reportFailure(
                    CheckinFailureReporter.Step.RESERVATIONS,
                    e.classify("예약 정보를 불러올 수 없습니다"),
                    startedAt,
                )
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
                // 이용권 또는 PASS형 체험권은 예약 없이 체크인 가능
                state.selectedTicketIsPass -> performCheckin()
                // 예약 차감형 수강권/체험권은 예약 선택 필수 → 방어적으로 무시
                else -> return@launch
            }
        }
    }

    private suspend fun performAttend() {
        val state = _uiState.value
        val reservationId = state.selectedReservationId ?: return
        val bearerToken = "Bearer ${token ?: return}"

        _uiState.value = state.copy(isLoading = true)
        val startedAt = SystemClock.elapsedRealtime()

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
            openDoorIfEnabled()
        } catch (e: Exception) {
            val failure = e.classify("출석 처리에 실패했습니다")
            Log.e("CHECKIN", "출석 처리 실패: ${failure.userMessage}", e)
            reportFailure(CheckinFailureReporter.Step.ATTEND, failure, startedAt)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = failure.userMessage,
            )
        }
    }

    /**
     * 선택 화면에서 퇴실이 아닌 쪽을 고른 경우.
     *
     * <p>재입장 자격이 있으면 무차감 재입장, 없으면 기존 이용권 선택 흐름으로 보낸다.
     */
    fun continueEntry() {
        val state = _uiState.value
        if (!state.needsAttendChoice) return

        if (state.canReentry) {
            _uiState.value = state.copy(isLoading = true, needsAttendChoice = false)
            viewModelScope.launch { performReentry(state.reentryMessage, restoreChoiceOnFailure = true) }
            return
        }

        // ── [비활성] 이용권 전용 회원 자동 체크인 ──────────────────────────────
        // loadTickets 의 같은 분기와 한 쌍이다. 되살릴 때는 반드시 둘 다 푼다.
        // 한쪽만 풀면 진입 경로에 따라 동작이 갈린다.
        //
        // val member = state.member ?: return
        // val activeTickets = member.tickets.filter { it.status != "INACTIVE" }
        // val activePasses = member.passes.filter { it.status != "INACTIVE" }
        // if (activeTickets.isEmpty() && activePasses.isNotEmpty()) {
        //     val pass = activePasses.first()
        //     _uiState.value = state.copy(
        //         isLoading = true,
        //         needsAttendChoice = false,
        //         selectedTicketId = pass.id,
        //         selectedTicketType = TicketType.COURSE_PASS,
        //         deductCount = 0,
        //     )
        //     viewModelScope.launch { performCheckin() }
        //     return
        // }
        // ──────────────────────────────────────────────────────────────────

        _uiState.value = state.copy(isLoading = false, needsAttendChoice = false)
    }

    /**
     * 선택 화면에서 퇴실을 고른 경우. 당일 입장 기록에 퇴실 시각을 남긴다.
     */
    fun checkout() {
        val state = _uiState.value
        if (!state.needsAttendChoice) return

        val currentToken = token
        val branchId = sessionManager.branchId
        if (currentToken == null || branchId == null) {
            // 여기서 조용히 빠지면 버튼이 먹통이 된 것처럼 보인다
            _uiState.value = state.copy(error = "퇴실 처리에 실패했습니다")
            return
        }

        // 중복 탭 방어. 상태 전환을 launch 밖에서 먼저 끝내 두 번째 탭이 위 가드에 걸리게 한다.
        _uiState.value = state.copy(isLoading = true, needsAttendChoice = false)

        viewModelScope.launch {
            val bearerToken = "Bearer $currentToken"
            val startedAt = SystemClock.elapsedRealtime()

            try {
                val response = api.checkout(
                    authorization = bearerToken,
                    adminToken = sessionManager.token,
                    request = CheckoutRequest(branchId = branchId),
                )
                Log.d("CHECKIN", "퇴실 처리 성공: ${response.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    checkoutDone = true,
                    checkinMessage = response.message,
                )
            } catch (e: Exception) {
                val failure = e.classify("퇴실 처리에 실패했습니다")
                Log.e("CHECKIN", "퇴실 처리 실패: ${failure.userMessage}", e)
                reportFailure(CheckinFailureReporter.Step.CHECKOUT, failure, startedAt)
                // 실패해도 선택 화면으로 되돌려 재시도·재입장을 고를 수 있게 한다
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    needsAttendChoice = true,
                    error = failure.userMessage,
                )
            }
        }
    }

    /**
     * 무차감 재입장 처리. 당일 출석 회원을 차감/만료 없이 입장시키고 출입문을 연다.
     */
    private suspend fun performReentry(
        reentryMessage: String?,
        restoreChoiceOnFailure: Boolean = false,
    ) {
        val bearerToken = "Bearer ${token ?: return}"
        val branchId = sessionManager.branchId ?: return
        val startedAt = SystemClock.elapsedRealtime()

        try {
            val response = api.reentry(
                authorization = bearerToken,
                adminToken = sessionManager.token,
                request = ReentryRequest(
                    branchId = branchId,
                    checkInMethod = checkInMethod,
                ),
            )
            Log.d("CHECKIN", "재입장 처리 성공: ${response.message}")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                checkinDone = true,
                checkinMessage = response.message ?: reentryMessage,
            )
            openDoorIfEnabled()
        } catch (e: Exception) {
            val failure = e.classify("재입장 처리에 실패했습니다")
            Log.e("CHECKIN", "재입장 처리 실패: ${failure.userMessage}", e)
            reportFailure(CheckinFailureReporter.Step.REENTRY, failure, startedAt)
            // 선택 화면에서 온 실패는 선택 화면으로 되돌린다. 그냥 두면 무차감 재입장 대상 회원이
            // 차감이 일어나는 이용권 선택 화면으로 떨어진다.
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                needsAttendChoice = restoreChoiceOnFailure,
                error = failure.userMessage,
            )
        }
    }

    private suspend fun performCheckin() {
        val state = _uiState.value
        val ticketId = state.selectedTicketId ?: return
        val ticketType = state.selectedTicketType ?: return
        val bearerToken = "Bearer ${token ?: return}"

        _uiState.value = state.copy(isLoading = true)
        val startedAt = SystemClock.elapsedRealtime()

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
                    checkinDone = true,
                    checkinMessage = body?.message,
                )
                openDoorIfEnabled()
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = try {
                    moshi.adapter(ErrorResponse::class.java)
                        .fromJson(errorBody ?: "")?.message
                } catch (_: Exception) {
                    null
                } ?: "체크인에 실패했습니다 (${response.code()})"

                Log.e("CHECKIN", "체크인 실패: $errorMsg")
                // 예외가 아니라 비-2xx 응답이라 catch로 오지 않는다. 여기서 직접 보고한다
                reportFailure(
                    CheckinFailureReporter.Step.CHECKIN,
                    CheckinFailure(
                        userMessage = errorMsg,
                        httpStatus = response.code(),
                        errorType = "HttpErrorResponse",
                        errorMessage = errorMsg,
                        errorBody = errorBody,
                    ),
                    startedAt,
                )
                _uiState.value = _uiState.value.copy(isLoading = false, error = errorMsg)
            }
        } catch (e: Exception) {
            val failure = e.classify("체크인에 실패했습니다")
            Log.e("CHECKIN", "체크인 실패: ${failure.userMessage}", e)
            reportFailure(CheckinFailureReporter.Step.CHECKIN, failure, startedAt)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = failure.userMessage,
            )
        }
    }

    /**
     * 체크인 성공 후 설정된 출입문을 best-effort로 1회 연다.
     * 실패해도 체크인 결과에는 영향을 주지 않고 로그만 남긴다.
     */
    private fun openDoorIfEnabled() {
        if (!settingsManager.doorOpenEnabled) return
        val sensorId = settingsManager.doorSensorId
        if (sensorId.isBlank()) return
        val bearerToken = "Bearer ${token ?: return}"
        viewModelScope.launch {
            val startedAt = SystemClock.elapsedRealtime()
            try {
                api.openDoor(bearerToken, OpenDoorRequest(sensorId))
                Log.d("CHECKIN", "출입문 열림 요청 성공: $sensorId")
            } catch (e: Exception) {
                Log.e("CHECKIN", "출입문 열기 실패(무시)", e)
                reportFailure(
                    CheckinFailureReporter.Step.OPEN_DOOR,
                    e.classify("출입문 열기에 실패했습니다"),
                    startedAt,
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /** 예외를 화면 문구와 서버 보고용 필드로 분해한다. */
    private fun Throwable.classify(fallback: String): CheckinFailure =
        toCheckinFailure(moshi, networkMonitor.isOnline(), fallback)

    /**
     * 실패를 서버 로그에 남긴다.
     *
     * @param startedAt 해당 API 호출 직전의 [SystemClock.elapsedRealtime]. 경과 시간이
     *     읽기 타임아웃(15초)에 근접하면 서버 지연, 짧으면 서버가 거부한 것으로 갈린다
     */
    private fun reportFailure(
        step: CheckinFailureReporter.Step,
        failure: CheckinFailure,
        startedAt: Long,
    ) {
        failureReporter.report(
            step = step,
            failure = failure,
            elapsedMs = SystemClock.elapsedRealtime() - startedAt,
            branchId = sessionManager.branchId,
            memberName = _uiState.value.member?.name,
            checkInMethod = checkInMethod,
        )
    }
}
