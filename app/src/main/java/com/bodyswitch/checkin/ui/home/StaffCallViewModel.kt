package com.bodyswitch.checkin.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bodyswitch.checkin.data.api.KioskApi
import com.bodyswitch.checkin.data.api.dto.StaffCallRequest
import com.bodyswitch.checkin.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class StaffCallState {
    object Idle : StaffCallState()
    object Loading : StaffCallState()
    data class Success(val message: String) : StaffCallState()
    data class Error(val message: String) : StaffCallState()
}

@HiltViewModel
class StaffCallViewModel @Inject constructor(
    private val api: KioskApi,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow<StaffCallState>(StaffCallState.Idle)
    val state: StateFlow<StaffCallState> = _state.asStateFlow()

    fun callStaff(phoneNumber: String) {
        if (phoneNumber.isBlank()) {
            _state.value = StaffCallState.Error("직원 연락처가 설정되지 않았습니다\n메인 설정에서 직원 연락처를 입력해 주세요")
            return
        }
        viewModelScope.launch {
            _state.value = StaffCallState.Loading
            try {
                val token = "Bearer ${sessionManager.token}"
                val response = api.staffCall(
                    authorization = token,
                    request = StaffCallRequest(phoneNumber = phoneNumber),
                )
                _state.value = StaffCallState.Success(response.message ?: "직원에게 알림을 전송했습니다")
            } catch (e: Exception) {
                _state.value = StaffCallState.Error("알림톡 전송에 실패했습니다")
            }
        }
    }

    fun resetState() {
        _state.value = StaffCallState.Idle
    }
}
