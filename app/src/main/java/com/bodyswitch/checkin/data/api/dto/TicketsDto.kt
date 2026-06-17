package com.bodyswitch.checkin.data.api.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TicketsResponse(
    val memberId: String,
    val memberName: String,
    val courseTickets: List<TicketDto>? = null,
    val trialTickets: List<TicketDto>? = null,
    val coursePasses: List<CoursePassDto>? = null,
)

@JsonClass(generateAdapter = true)
data class TicketDto(
    val id: Long,
    val ticketName: String,
    val usageCount: Int,
    val usedCount: Int,
    val remainCount: Int,
    val startDate: String?,
    val expireDate: String?,
    val ticketType: String,
    // 체험권 수업 유형(PASS면 이용권형 → 횟수 차감/예약 없이 입장). 수강권/레슨형은 null 또는 PASS 외 값.
    val classType: String? = null,
    val status: String? = null,
)

@JsonClass(generateAdapter = true)
data class CoursePassDto(
    val id: Long,
    val passName: String,
    val startDate: String?,
    val expireDate: String?,
    val ticketType: String,
    val status: String? = null,
)
