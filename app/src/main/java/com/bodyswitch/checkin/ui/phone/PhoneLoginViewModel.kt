package com.bodyswitch.checkin.ui.phone

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bodyswitch.checkin.data.api.KioskApi
import com.bodyswitch.checkin.data.api.dto.ErrorResponse
import com.bodyswitch.checkin.data.api.dto.MemberCandidate
import com.bodyswitch.checkin.data.api.dto.PhoneLoginCandidatesResponse
import com.bodyswitch.checkin.data.api.dto.PhoneLoginRequest
import com.bodyswitch.checkin.data.network.NetworkMonitor
import com.bodyswitch.checkin.data.session.EmployeeLoginHolder
import com.bodyswitch.checkin.data.session.SessionManager
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PhoneLoginUiState(
    val phoneNumber: String = "",
    val isLoading: Boolean = false,
    val token: String? = null,
    val error: String? = null,
    val loginDispatched: Boolean = false, // 자동 로그인 중복 실행 방지
    // 직원 → 선택 화면으로 이동
    val isEmployee: Boolean = false,
    // 뒤 4자리가 겹치는 사람이 여럿일 때만 채워진다
    val candidates: List<MemberCandidate> = emptyList(),
)

@HiltViewModel
class PhoneLoginViewModel @Inject constructor(
    private val api: KioskApi,
    private val sessionManager: SessionManager,
    private val moshi: Moshi,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhoneLoginUiState())
    val uiState: StateFlow<PhoneLoginUiState> = _uiState.asStateFlow()

    fun onPhoneNumberChange(value: String) {
        _uiState.value = _uiState.value.copy(phoneNumber = value, loginDispatched = false)
    }

    fun login() {
        val phone = _uiState.value.phoneNumber
        if (phone.length != PHONE_DIGITS) return
        requestLogin(phone, candidateId = null)
    }

    /** 후보 선택 화면에서 본인을 고른 뒤 그 후보로 다시 로그인한다 */
    fun selectCandidate(candidateId: String) {
        val phone = _uiState.value.phoneNumber
        if (phone.length != PHONE_DIGITS || _uiState.value.isLoading) return
        requestLogin(phone, candidateId = candidateId)
    }

    /** 후보 선택을 취소하고 번호 입력으로 되돌린다 */
    fun clearCandidates() {
        _uiState.value = _uiState.value.copy(
            candidates = emptyList(),
            phoneNumber = "",
            loginDispatched = false,
        )
    }

    private fun requestLogin(last4: String, candidateId: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                loginDispatched = true,
            )

            try {
                Log.d("CHECKIN", "전화번호 뒤 4자리 로그인 시도: $last4, candidateId=$candidateId")
                val response = api.phoneLogin(
                    adminToken = sessionManager.token,
                    request = PhoneLoginRequest(
                        phoneLast4 = last4,
                        allowCandidates = true,
                        candidateId = candidateId,
                    ),
                )
                Log.d("CHECKIN", "로그인 성공: ${response.name}, role=${response.role}")

                if (response.role == "EMPLOYEE") {
                    EmployeeLoginHolder.set(
                        token = response.token,
                        branchId = response.branchId ?: sessionManager.branchId ?: return@launch,
                        employeeName = response.name,
                        checkInMethod = "PHONE",
                    )
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isEmployee = true,
                        candidates = emptyList(),
                    )
                    return@launch
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    token = response.token,
                    candidates = emptyList(),
                )
            } catch (e: retrofit2.HttpException) {
                if (e.code() == HTTP_CONFLICT) {
                    val candidates = parseCandidates(e)
                    if (candidates.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            candidates = candidates,
                        )
                        return@launch
                    }
                }
                val errorBody = e.response()?.errorBody()?.string()
                val serverMessage = try {
                    moshi.adapter(ErrorResponse::class.java)
                        .fromJson(errorBody ?: "")?.message
                } catch (_: Exception) {
                    null
                }
                val errorMsg = when (e.code()) {
                    404 -> "회원을 찾을 수 없습니다"
                    401 -> "인증에 실패했습니다"
                    406 -> serverMessage ?: "해당 지점의 회원이 아닙니다"
                    else -> serverMessage ?: "로그인 실패 (${e.code()})"
                }
                Log.e("CHECKIN", "전화번호 로그인 실패", e)
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

    // 409 본문은 errorBody라 한 번만 읽을 수 있다. 후보 파싱에 실패하면 일반 에러로 흘린다.
    private fun parseCandidates(e: retrofit2.HttpException): List<MemberCandidate> = try {
        val body = e.response()?.errorBody()?.string()
        moshi.adapter(PhoneLoginCandidatesResponse::class.java)
            .fromJson(body ?: "")?.candidates.orEmpty()
    } catch (ex: Exception) {
        Log.w("CHECKIN", "후보 목록 파싱 실패", ex)
        emptyList()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearToken() {
        _uiState.value = _uiState.value.copy(
            token = null,
            phoneNumber = "",
            candidates = emptyList(),
        )
    }

    fun clearEmployee() {
        _uiState.value = _uiState.value.copy(
            isEmployee = false,
            phoneNumber = "",
            candidates = emptyList(),
        )
    }

    companion object {
        const val PHONE_DIGITS = 4
        private const val HTTP_CONFLICT = 409
    }
}
