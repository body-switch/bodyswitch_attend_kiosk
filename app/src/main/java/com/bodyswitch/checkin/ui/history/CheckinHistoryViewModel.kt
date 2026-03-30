package com.bodyswitch.checkin.ui.history

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bodyswitch.checkin.data.api.KioskApi
import com.bodyswitch.checkin.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class AttendanceRecord(
    val id: Long,
    val memberId: String,
    val memberName: String,
    val phoneNumber: String?,
    val branchName: String,
    val attendType: String,
    val attendCount: Int,
    val startTime: String,
    val endTime: String?,
    val date: String,
    // 이용권 정보 (checkin history에서)
    val ticketName: String? = null,
    val usageCount: Int? = null,
    val remainCount: Int? = null,
    val status: String = "이용중",
)

data class HistoryUiState(
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val selectedDate: LocalDate = LocalDate.now(),
    val records: List<AttendanceRecord> = emptyList(),
    val totalCount: Int = 0,
    val page: Int = 1,
    val error: String? = null,
    val showDatePicker: Boolean = false,
)

@HiltViewModel
class CheckinHistoryViewModel @Inject constructor(
    private val api: KioskApi,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val pageSize = 20

    init {
        loadData()
    }

    fun onSearchQueryChange(value: String) {
        _uiState.value = _uiState.value.copy(searchQuery = value)
    }

    fun search() {
        _uiState.value = _uiState.value.copy(page = 1, records = emptyList())
        loadData()
    }

    fun clearSearch() {
        _uiState.value = _uiState.value.copy(searchQuery = "", page = 1, records = emptyList())
        loadData()
    }

    fun selectDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(
            selectedDate = date,
            showDatePicker = false,
            page = 1,
            records = emptyList(),
        )
        loadData()
    }

    fun toggleDatePicker() {
        _uiState.value = _uiState.value.copy(showDatePicker = !_uiState.value.showDatePicker)
    }

    fun dismissDatePicker() {
        _uiState.value = _uiState.value.copy(showDatePicker = false)
    }

    private fun loadData(page: Int = 1) {
        val token = sessionManager.token ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val date = _uiState.value.selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val searchInput = _uiState.value.searchQuery.trim().ifBlank { null }

                val response = api.getAttendance(
                    authorization = "Bearer $token",
                    branchId = sessionManager.branchId,
                    searchInput = searchInput,
                    startDate = date,
                    endDate = date,
                    page = page,
                    limit = pageSize,
                )

                Log.d("HISTORY", "출입 기록 조회 성공: ${response.totalElements}건")

                val records = response.content.map { item ->
                    AttendanceRecord(
                        id = item.id,
                        memberId = item.memberId,
                        memberName = item.memberName,
                        phoneNumber = item.phoneNumber,
                        branchName = item.branchName,
                        attendType = item.attendType,
                        attendCount = item.attendCount,
                        startTime = item.startTime,
                        endTime = item.endTime,
                        date = item.writeDate,
                        status = if (item.endTime != null) "퇴실" else "이용중",
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    records = if (page == 1) records else _uiState.value.records + records,
                    totalCount = response.totalElements,
                    page = page,
                )
            } catch (e: retrofit2.HttpException) {
                val errorMsg = when (e.code()) {
                    401 -> "인증이 만료되었습니다"
                    else -> "조회 실패 (${e.code()})"
                }
                Log.e("HISTORY", "출입 기록 조회 실패", e)
                _uiState.value = _uiState.value.copy(isLoading = false, error = errorMsg)
            } catch (e: Exception) {
                Log.e("HISTORY", "네트워크 오류", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "서버에 연결할 수 없습니다",
                )
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.records.size < state.totalCount && !state.isLoading) {
            loadData(state.page + 1)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(page = 1, records = emptyList())
        loadData()
    }
}
