package com.bodyswitch.checkin.data.api.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CheckinHistoryResponse(
    val totalElements: Int,
    val number: Int,
    val size: Int,
    val content: List<CheckinHistoryItem>,
)

@JsonClass(generateAdapter = true)
data class CheckinHistoryItem(
    val id: Long,
    val memberId: String,
    val memberName: String,
    val phoneNumber: String?,
    val checkInMethod: String,
    val ticketType: String,
    val ticketName: String,
    val deductCount: Int?,
    val remainCount: Int?,
    val status: String,
    val writeDate: String,
    val writeTime: String,
)

@JsonClass(generateAdapter = true)
data class AttendanceResponse(
    val totalElements: Int,
    val number: Int,
    val size: Int,
    val content: List<AttendanceItem>,
)

@JsonClass(generateAdapter = true)
data class AttendanceItem(
    val id: Long,
    val memberId: String,
    val memberName: String,
    val phoneNumber: String?,
    val branchName: String,
    val attendType: String,
    val attendCount: Int,
    val writeDate: String,
    val startTime: String,
    val endTime: String?,
    // 이용권 단위 입장 내역. member_entry_event 적재(2026-08-18) 이전 날짜는 비어 있다.
    val entries: List<AttendanceEntry> = emptyList(),
)

// 이용권 1건 기준 입장. 퇴실 시각은 그날 하나를 공유하고, 체류시간은 입장 건별로 계산된다.
@JsonClass(generateAdapter = true)
data class AttendanceEntry(
    val eventType: String?,
    val enterTime: String?,
    val exitTime: String?,
    val staySeconds: Long?,
    val ticketType: String?,
    val ticketName: String?,
)

// 직원 출입 기록
@JsonClass(generateAdapter = true)
data class EmployeeAttendHistoryResponse(
    val totalElements: Int,
    val number: Int,
    val size: Int,
    val content: List<EmployeeAttendHistoryItem>,
)

@JsonClass(generateAdapter = true)
data class EmployeeAttendHistoryItem(
    val id: Long,
    val employeeId: Long,
    val employeeName: String,
    val phoneNumber: String?,
    val branchId: Long,
    val writeDate: String,
    val startTime: String?,
    val endTime: String?,
    val entryCount: Int = 0,
    val exitCount: Int = 0,
)

@JsonClass(generateAdapter = true)
data class CancelCheckinRequest(
    val checkInLogId: Long,
)

@JsonClass(generateAdapter = true)
data class CancelCheckinResponse(
    val success: Boolean,
    val message: String?,
    val checkInLogId: Long,
    val status: String,
)
