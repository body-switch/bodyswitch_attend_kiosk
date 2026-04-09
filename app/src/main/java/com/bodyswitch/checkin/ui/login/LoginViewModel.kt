package com.bodyswitch.checkin.ui.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bodyswitch.checkin.data.api.KioskApi
import com.bodyswitch.checkin.data.api.dto.AdminLoginRequest
import com.bodyswitch.checkin.data.session.AutoLoginManager
import com.bodyswitch.checkin.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val autoLogin: Boolean = false,
    val isLoading: Boolean = false,
    val loginSuccess: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val api: KioskApi,
    private val sessionManager: SessionManager,
    private val autoLoginManager: AutoLoginManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var autoLoginAttempted = false

    init {
        // 저장된 자동로그인 정보 복원
        if (autoLoginManager.isEnabled) {
            val savedUsername = autoLoginManager.username ?: ""
            val savedPassword = autoLoginManager.password ?: ""
            _uiState.value = LoginUiState(
                username = savedUsername,
                password = savedPassword,
                autoLogin = true,
            )
            // 자동 로그인 시도 (1회만)
            if (!autoLoginAttempted && savedUsername.isNotBlank() && savedPassword.isNotBlank()) {
                autoLoginAttempted = true
                performLogin(savedUsername, savedPassword)
            }
        }
    }

    fun onUsernameChange(value: String) {
        _uiState.value = _uiState.value.copy(username = value)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value)
    }

    fun onAutoLoginChange(value: Boolean) {
        _uiState.value = _uiState.value.copy(autoLogin = value)
        if (!value) {
            autoLoginManager.clear()
        }
    }

    fun login() {
        val state = _uiState.value
        if (state.username.isBlank() || state.password.isBlank()) return
        performLogin(state.username, state.password)
    }

    private fun performLogin(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                Log.d("LOGIN", "관리자 로그인 시도: $username")
                val response = api.adminLogin(
                    AdminLoginRequest(username = username, password = password)
                )

                Log.d("LOGIN", "로그인 응답 - role: ${response.userRole}, branch: ${response.branchName}")

                if (response.userRole !in SessionManager.ALLOWED_ROLES) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "접근 권한이 없습니다 (${response.userRole})",
                    )
                    return@launch
                }

                sessionManager.login(
                    token = response.token,
                    username = response.username ?: username,
                    name = response.name,
                    userRole = response.userRole,
                    branchId = response.branchId,
                    branchName = response.branchName,
                    centerId = response.centerId,
                    businessName = response.businessName,
                    centerType = response.centerType,
                )

                // 자동로그인 체크 시 자격증명 저장
                if (_uiState.value.autoLogin) {
                    autoLoginManager.save(username, password)
                } else {
                    autoLoginManager.clear()
                }

                try {
                    val branchInfo = api.getBranchInfo("Bearer ${response.token}")
                    sessionManager.updateBranchInfo(branchInfo)
                    Log.d("LOGIN", "지점 정보: ${branchInfo.branchName} ${branchInfo.address}")
                } catch (e: Exception) {
                    Log.w("LOGIN", "지점 정보 조회 실패 (무시)", e)
                }

                _uiState.value = _uiState.value.copy(isLoading = false, loginSuccess = true)
            } catch (e: retrofit2.HttpException) {
                val errorMsg = when (e.code()) {
                    401 -> "아이디 또는 비밀번호가 올바르지 않습니다"
                    406 -> "계정이 비활성 상태입니다. 관리자에게 문의하세요"
                    else -> "로그인 실패 (${e.code()})"
                }
                Log.e("LOGIN", "로그인 실패", e)
                _uiState.value = _uiState.value.copy(isLoading = false, error = errorMsg)
            } catch (e: Exception) {
                Log.e("LOGIN", "네트워크 오류", e)
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
}
