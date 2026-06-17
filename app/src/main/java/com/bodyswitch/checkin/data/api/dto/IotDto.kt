package com.bodyswitch.checkin.data.api.dto

import com.squareup.moshi.JsonClass

// 출입문 목록 응답 (connected=false면 IoT 미연동 지점)
@JsonClass(generateAdapter = true)
data class DoorListResponse(
    val connected: Boolean,
    val doors: List<DoorInfo> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class DoorInfo(
    val roomName: String?,
    val sensorId: String,
)

@JsonClass(generateAdapter = true)
data class OpenDoorRequest(
    val sensorId: String,
)
