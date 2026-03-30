package com.bodyswitch.checkin.ui.phone

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bodyswitch.checkin.data.api.KioskApi
import com.bodyswitch.checkin.data.api.dto.PhoneLoginRequest
import com.bodyswitch.checkin.data.session.SessionManager
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
)

@HiltViewModel
class PhoneLoginViewModel @Inject constructor(
    private val api: KioskApi,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhoneLoginUiState())
    val uiState: StateFlow<PhoneLoginUiState> = _uiState.asStateFlow()

    fun onPhoneNumberChange(value: String) {
        _uiState.value = _uiState.value.copy(phoneNumber = value, loginDispatched = false)
    }

    fun login() {
        val phone = _uiState.value.phoneNumber
        if (phone.length != 8) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, loginDispatched = true)

            try {
                val formatted = "010-${phone.substring(0, 4)}-${phone.substring(4)}"
                Log.d("CHECKIN", "전화번호 로그인 시도: $formatted")
                val response = api.phoneLogin(
                    adminToken = sessionManager.token,
                    request = PhoneLoginRequest(phoneNumber = formatted),
                )
                Log.d("CHECKIN", "로그인 성공: ${response.name}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    token = response.token,
                )
            } catch (e: retrofit2.HttpException) {
                val errorMsg = when (e.code()) {
                    404 -> "회원을 찾을 수 없습니다"
                    401 -> "인증에 실패했습니다"
                    406 -> "해당 지점의 회원이 아닙니다"
                    else -> "로그인 실패 (${e.code()})"
                }
                Log.e("CHECKIN", "전화번호 로그인 실패", e)
                _uiState.value = _uiState.value.copy(isLoading = false, error = errorMsg)
            } catch (e: Exception) {
                Log.e("CHECKIN", "네트워크 오류", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "서버에 연결할 수 없습니다",
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearToken() {
        _uiState.value = _uiState.value.copy(token = null, phoneNumber = "")
    }
}
