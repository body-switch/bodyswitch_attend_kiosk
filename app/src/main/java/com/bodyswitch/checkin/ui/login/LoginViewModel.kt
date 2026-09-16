package com.bodyswitch.checkin.ui.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bodyswitch.checkin.data.api.KioskApi
import com.bodyswitch.checkin.data.api.dto.AdminLoginRequest
import com.bodyswitch.checkin.data.api.dto.ErrorResponse
import com.bodyswitch.checkin.data.network.NetworkMonitor
import com.bodyswitch.checkin.data.session.AutoLoginManager
import com.bodyswitch.checkin.data.session.SessionManager
import com.squareup.moshi.Moshi
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
    private val networkMonitor: NetworkMonitor,
    private val moshi: Moshi,
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
                // 406 은 서버가 사유를 그대로 준다 — 체크인앱 미승인 / 매니저 계정 아님 / 계정 상태.
                // 하나로 뭉개면 센터가 무엇을 해야 하는지 알 수 없다.
                val errorMsg = when (e.code()) {
                    401 -> "아이디 또는 비밀번호가 올바르지 않습니다"
                    406 -> serverMessage(e) ?: "로그인할 수 없는 계정입니다. 관리자에게 문의하세요"
                    else -> "로그인 실패 (${e.code()})"
                }
                Log.e("LOGIN", "로그인 실패", e)
                _uiState.value = _uiState.value.copy(isLoading = false, error = errorMsg)
            } catch (e: Exception) {
                Log.e("LOGIN", "네트워크 오류", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = networkMonitor.networkErrorMessage(),
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun serverMessage(e: retrofit2.HttpException): String? = try {
        val body = e.response()?.errorBody()?.string()
        moshi.adapter(ErrorResponse::class.java).fromJson(body ?: "")?.message
    } catch (_: Exception) {
        null
    }
}
