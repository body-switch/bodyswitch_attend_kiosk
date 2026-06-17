package com.bodyswitch.checkin.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bodyswitch.checkin.data.api.KioskApi
import com.bodyswitch.checkin.data.api.dto.DoorInfo
import com.bodyswitch.checkin.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DoorSettingUiState(
    val loading: Boolean = true,
    val connected: Boolean = false,
    val doors: List<DoorInfo> = emptyList(),
    val loadFailed: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val api: KioskApi,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _doorState = MutableStateFlow(DoorSettingUiState())
    val doorState: StateFlow<DoorSettingUiState> = _doorState.asStateFlow()

    init {
        loadDoors()
    }

    private fun loadDoors() {
        val token = sessionManager.token
        val branchId = sessionManager.branchId
        if (token == null || branchId == null) {
            _doorState.value = DoorSettingUiState(loading = false, connected = false)
            return
        }
        viewModelScope.launch {
            try {
                val response = api.getDoors("Bearer $token", branchId)
                _doorState.value = DoorSettingUiState(
                    loading = false,
                    connected = response.connected,
                    doors = response.doors,
                )
            } catch (e: Exception) {
                Log.e("SETTINGS", "출입문 목록 조회 실패", e)
                _doorState.value = DoorSettingUiState(loading = false, connected = false, loadFailed = true)
            }
        }
    }
}
