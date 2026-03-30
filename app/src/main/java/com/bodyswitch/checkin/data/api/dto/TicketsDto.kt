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
