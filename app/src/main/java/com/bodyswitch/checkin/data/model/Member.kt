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
    // 이용권형 체험권: 예약 없이 기간 내 입장만 하는 이용권처럼 동작한다.
    // 예약 필요 여부는 classType 으로만 판정한다 (PASS = 자유입장, 레슨형은 예약 필요).
    // usageType(PERIOD/COUNT)은 차감 여부일 뿐 예약 필요 여부와 무관하므로 조건에 넣지 않는다.
    val isPassType: Boolean
        get() = type == TicketType.TRIAL_TICKET && classType == "PASS"
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
