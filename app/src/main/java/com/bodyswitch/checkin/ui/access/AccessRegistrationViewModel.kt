package com.bodyswitch.checkin.ui.access

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bodyswitch.checkin.data.api.KioskApi
import com.bodyswitch.checkin.data.api.dto.ErrorResponse
import com.bodyswitch.checkin.data.api.dto.FaceRegistrationRequest
import com.bodyswitch.checkin.data.api.dto.PhoneLoginRequest
import com.bodyswitch.checkin.data.api.dto.QrIssuanceRequest
import com.bodyswitch.checkin.data.network.NetworkMonitor
import com.bodyswitch.checkin.data.session.SessionManager
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// 플로우 단계: phone → (상품O) confirm → method → face/QR → done, (상품X) phone → noproduct
enum class AccessStep { PHONE, CONFIRM, METHOD, FACE, NO_PRODUCT, DONE }

enum class AccessIssueType { FACE, QR }

data class AccessRegistrationUiState(
    val step: AccessStep = AccessStep.PHONE,
    val digits: String = "",
    val isLoading: Boolean = false,
    // 안면 촬영~등록 API 응답까지 오버레이 표시
    val isRegistering: Boolean = false,
    val error: String? = null,
    val memberName: String = "",
    val issueType: AccessIssueType = AccessIssueType.FACE,
    val qrPayload: String? = null,
    val accessGranted: Boolean = true,
)

@HiltViewModel
class AccessRegistrationViewModel @Inject constructor(
    private val api: KioskApi,
    private val sessionManager: SessionManager,
    private val moshi: Moshi,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccessRegistrationUiState())
    val uiState: StateFlow<AccessRegistrationUiState> = _uiState.asStateFlow()

    // 회원 토큰 (phone-login 응답). Bearer 인증에 사용.
    private var memberToken: String? = null

    fun onDigit(digit: String) {
        _uiState.update {
            if (it.digits.length < PHONE_DIGITS) it.copy(digits = it.digits + digit) else it
        }
    }

    fun onDelete() {
        _uiState.update { it.copy(digits = it.digits.dropLast(1)) }
    }

    fun onClear() {
        _uiState.update { it.copy(digits = "") }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // 상단 출입등록 버튼 = 언제든 phone부터 재시작
    fun restart() {
        memberToken = null
        _uiState.value = AccessRegistrationUiState()
    }

    // 뒤로가기 매핑: confirm→phone, method→confirm, face→method, noproduct→phone.
    // phone에서는 false 반환 → 화면 이탈(home).
    fun goBack(): Boolean {
        val state = _uiState.value
        return when (state.step) {
            AccessStep.PHONE -> false
            AccessStep.CONFIRM -> {
                memberToken = null
                _uiState.update { it.copy(step = AccessStep.PHONE, digits = "") }
                true
            }
            AccessStep.METHOD -> {
                _uiState.update { it.copy(step = AccessStep.CONFIRM) }
                true
            }
            AccessStep.FACE -> {
                _uiState.update { it.copy(step = AccessStep.METHOD) }
                true
            }
            AccessStep.NO_PRODUCT -> {
                memberToken = null
                _uiState.update { it.copy(step = AccessStep.PHONE, digits = "") }
                true
            }
            AccessStep.DONE -> false
        }
    }

    // 전화번호 뒤 8자리로 회원 조회 후 상품 보유 여부에 따라 confirm / noproduct 분기
    fun submitPhone() {
        val phone = _uiState.value.digits
        if (phone.length != PHONE_DIGITS || _uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val formatted = "010-${phone.substring(0, 4)}-${phone.substring(4)}"
                val login = api.phoneLogin(
                    adminToken = sessionManager.token,
                    request = PhoneLoginRequest(phoneNumber = formatted),
                )

                // 회원만 출입등록 대상 (직원 제외 정책)
                if (login.role == "EMPLOYEE") {
                    _uiState.update {
                        it.copy(isLoading = false, digits = "", error = "직원은 출입등록 대상이 아닙니다")
                    }
                    return@launch
                }

                val tickets = api.getTickets(
                    authorization = "Bearer ${login.token}",
                    branchId = sessionManager.branchId,
                )
                val hasProduct = !tickets.courseTickets.isNullOrEmpty() ||
                    !tickets.trialTickets.isNullOrEmpty() ||
                    !tickets.coursePasses.isNullOrEmpty()

                memberToken = login.token
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        memberName = login.name,
                        step = if (hasProduct) AccessStep.CONFIRM else AccessStep.NO_PRODUCT,
                    )
                }
            } catch (e: retrofit2.HttpException) {
                val errorMsg = when (e.code()) {
                    404 -> "등록된 회원이 없습니다"
                    401 -> "인증에 실패했습니다"
                    406 -> "해당 지점의 회원이 아닙니다"
                    else -> serverMessage(e) ?: "회원 조회 실패 (${e.code()})"
                }
                Log.e(TAG, "출입등록 회원 조회 실패", e)
                _uiState.update { it.copy(isLoading = false, error = errorMsg) }
            } catch (e: Exception) {
                Log.e(TAG, "네트워크 오류", e)
                _uiState.update { it.copy(isLoading = false, error = networkMonitor.networkErrorMessage()) }
            }
        }
    }

    fun confirmMember() {
        _uiState.update { it.copy(step = AccessStep.METHOD) }
    }

    fun selectFace() {
        _uiState.update { it.copy(step = AccessStep.FACE) }
    }

    // QR 선택 = 확인/촬영 없이 즉시 발급 → done
    fun selectQr() {
        val token = memberToken ?: return
        val branchId = sessionManager.branchId
        if (branchId == null) {
            _uiState.update { it.copy(error = "지점 정보를 확인할 수 없습니다") }
            return
        }
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = api.issueQr(
                    authorization = "Bearer $token",
                    request = QrIssuanceRequest(branchId = branchId),
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        step = AccessStep.DONE,
                        issueType = AccessIssueType.QR,
                        qrPayload = response.qrPayload,
                        accessGranted = response.accessGranted,
                    )
                }
            } catch (e: retrofit2.HttpException) {
                Log.e(TAG, "QR 발급 실패", e)
                _uiState.update {
                    it.copy(isLoading = false, error = serverMessage(e) ?: "QR 발급에 실패했습니다 (${e.code()})")
                }
            } catch (e: Exception) {
                Log.e(TAG, "네트워크 오류", e)
                _uiState.update { it.copy(isLoading = false, error = networkMonitor.networkErrorMessage()) }
            }
        }
    }

    // 촬영 버튼 → 오버레이 시작 (촬영~인코딩~API 응답까지 유지)
    fun startCapture() {
        _uiState.update { it.copy(isRegistering = true, error = null) }
    }

    fun onCaptureError() {
        _uiState.update { it.copy(isRegistering = false, error = "촬영에 실패했습니다. 다시 시도해 주세요") }
    }

    // 안면등록 API. 406 = 이미지 품질 문제 → 에러 표시 후 재촬영 유도.
    fun registerFace(authImage: String) {
        val token = memberToken ?: return
        val branchId = sessionManager.branchId
        if (branchId == null) {
            _uiState.update { it.copy(isRegistering = false, error = "지점 정보를 확인할 수 없습니다") }
            return
        }

        viewModelScope.launch {
            try {
                val response = api.registerFace(
                    authorization = "Bearer $token",
                    request = FaceRegistrationRequest(branchId = branchId, authImage = authImage),
                )
                _uiState.update {
                    it.copy(
                        isRegistering = false,
                        step = AccessStep.DONE,
                        issueType = AccessIssueType.FACE,
                        accessGranted = response.accessGranted,
                    )
                }
            } catch (e: retrofit2.HttpException) {
                val errorMsg = if (e.code() == 406) {
                    serverMessage(e) ?: "얼굴을 인식하지 못했습니다. 다시 촬영해 주세요"
                } else {
                    serverMessage(e) ?: "안면등록에 실패했습니다 (${e.code()})"
                }
                Log.e(TAG, "안면등록 실패", e)
                _uiState.update { it.copy(isRegistering = false, error = errorMsg) }
            } catch (e: Exception) {
                Log.e(TAG, "네트워크 오류", e)
                _uiState.update { it.copy(isRegistering = false, error = networkMonitor.networkErrorMessage()) }
            }
        }
    }

    private fun serverMessage(e: retrofit2.HttpException): String? =
        try {
            val errorBody = e.response()?.errorBody()?.string()
            moshi.adapter(ErrorResponse::class.java).fromJson(errorBody ?: "")?.message
        } catch (_: Exception) {
            null
        }

    companion object {
        private const val TAG = "ACCESS_REG"
        const val PHONE_DIGITS = 8
    }
}
