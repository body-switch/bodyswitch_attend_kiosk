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
