package com.bodyswitch.checkin.data.api.dto

import com.squareup.moshi.JsonClass

/**
 * 앱에서 발생한 체크인 실패를 서버 로그에 남기기 위한 보고.
 *
 * 서버는 이 값을 `[KIOSK-CLIENT]` WARN 로그 한 줄로 기록한다.
 * `httpStatus`가 null이면 서버에 닿지 못한 것이고, `elapsedMs`가 읽기 타임아웃(15초)에
 * 근접하면 서버가 늦게 응답한 것이다.
 */
@JsonClass(generateAdapter = true)
data class CheckinFailureReportRequest(
    val step: String,
    val branchId: Long?,
    val memberName: String?,
    val checkInMethod: String?,
    val httpStatus: Int?,
    val errorType: String?,
    val errorMessage: String?,
    val errorBody: String?,
    val elapsedMs: Long,
)
