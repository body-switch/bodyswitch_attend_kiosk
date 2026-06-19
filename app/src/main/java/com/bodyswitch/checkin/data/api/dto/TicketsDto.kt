package com.bodyswitch.checkin.data.api.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TicketsResponse(
    val memberId: String,
    val memberName: String,
    val courseTickets: List<TicketDto>? = null,
    val trialTickets: List<TicketDto>? = null,
    val coursePasses: List<CoursePassDto>? = null,
    // 무차감 재입장 정보. eligible=true면 이용권/예약 선택 없이 바로 재입장 처리.
    val reentry: ReentryInfo? = null,
)

// 당일 출석/입장 이력이 있는 회원의 무차감 재입장 정보
@JsonClass(generateAdapter = true)
data class ReentryInfo(
    val eligible: Boolean = false,
    val type: String? = null,
    val untilTime: String? = null,
    val message: String? = null,
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
    // 일일권 이용구분(PERIOD=기간제면 이용권형 → 차감/잔여횟수/예약 없이 기간 내 입장. COUNT=횟수제).
    val usageType: String? = null,
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
