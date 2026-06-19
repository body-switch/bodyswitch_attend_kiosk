package com.bodyswitch.checkin.data.api.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CheckinRequest(
    val ticketType: String,
    val ticketId: Long,
    val deductCount: Int = 1,
    val checkInMethod: String = "QR",
    val branchId: Long? = null,
)

@JsonClass(generateAdapter = true)
data class CheckinResponse(
    val success: Boolean,
    val message: String?,
    val ticketName: String?,
    val deductedCount: Int?,
    val remainCount: Int?,
    val usageCount: Int?,
)

@JsonClass(generateAdapter = true)
data class ErrorResponse(
    val message: String?,
    val status: Int?,
)

// 예약 조회
@JsonClass(generateAdapter = true)
data class ReservationsResponse(
    val memberId: String,
    val memberName: String,
    val ticketType: String,
    val ticketId: Long,
    val ticketName: String,
    val reservations: List<ReservationDto>,
)

@JsonClass(generateAdapter = true)
data class ReservationDto(
    val reservationId: Long,
    val courseClassName: String,
    val classDate: String,
    val startTime: String,
    val endTime: String,
    val roomName: String?,
    val employeeName: String?,
    val classType: String?,
    val status: String,
    val ticketName: String?,
)

// 직원 호출 알림톡
@JsonClass(generateAdapter = true)
data class StaffCallRequest(
    val phoneNumber: String,
)

@JsonClass(generateAdapter = true)
data class StaffCallResponse(
    val message: String?,
)

// 무차감 재입장 (당일 출석 회원)
@JsonClass(generateAdapter = true)
data class ReentryRequest(
    val branchId: Long,
    val checkInMethod: String,
)

@JsonClass(generateAdapter = true)
data class ReentryResponse(
    val success: Boolean,
    val message: String?,
    val type: String?,
    val untilTime: String?,
)

// 예약 출석
@JsonClass(generateAdapter = true)
data class AttendRequest(
    val branchId: Long,
    val reservationId: Long,
    val checkInMethod: String,
)

@JsonClass(generateAdapter = true)
data class AttendReservationInfo(
    val reservationId: Long,
    val courseClassName: String,
    val classDate: String,
    val startTime: String,
    val endTime: String,
    val roomName: String?,
    val employeeName: String?,
    val classType: String?,
    val status: String,
    val ticketName: String?,
)

@JsonClass(generateAdapter = true)
data class AttendResponse(
    val success: Boolean,
    val message: String?,
    val alreadyAttended: Boolean,
    val ticketName: String?,
    val deductedCount: Int?,
    val remainCount: Int?,
    val usageCount: Int?,
    val reservationInfo: AttendReservationInfo?,
)

// 직원 체크인
@JsonClass(generateAdapter = true)
data class EmployeeCheckinRequest(
    val branchId: Long,
    val checkInMethod: String = "QR",
    val attendType: String = "ENTRY",
    val memo: String? = null,
)

@JsonClass(generateAdapter = true)
data class EmployeeCheckinResponse(
    val employeeId: Long,
    val employeeName: String,
    val branchId: Long,
    val attendType: String = "ENTRY",
    val entryCount: Int = 0,
    val exitCount: Int = 0,
    val checkinTime: String = "",
    val message: String = "",
)
