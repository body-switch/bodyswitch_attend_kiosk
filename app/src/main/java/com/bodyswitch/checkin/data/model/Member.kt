package com.bodyswitch.checkin.data.model

data class Member(
    val id: String,
    val name: String,
    val tickets: List<Ticket>,
    val passes: List<CoursePass>,
)

data class Ticket(
    val id: Long,
    val name: String,
    val type: TicketType,
    val usageCount: Int,
    val remainCount: Int,
    val startDate: String?,
    val expireDate: String?,
    val classType: String? = null,
    val usageType: String? = null,
    val status: String? = null,
) {
    // 이용권형 일일권: 횟수 차감/예약 없이 기간 내 입장만 하는 이용권처럼 동작
    // - classType == "PASS" (PASS형 체험권)
    // - usageType == "PERIOD" (기간제 일일권)
    val isPassType: Boolean
        get() = type == TicketType.TRIAL_TICKET && (classType == "PASS" || usageType == "PERIOD")
}

enum class TicketType(val apiValue: String) {
    COURSE_TICKET("COURSE_TICKET"),
    TRIAL_TICKET("TRIAL_TICKET"),
    COURSE_PASS("COURSE_PASS"),
}

data class CoursePass(
    val id: Long,
    val name: String,
    val startDate: String?,
    val expireDate: String?,
    val status: String? = null,
)

data class Reservation(
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
