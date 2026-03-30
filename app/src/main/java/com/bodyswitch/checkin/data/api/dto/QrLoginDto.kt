package com.bodyswitch.checkin.data.api.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class QrLoginRequest(
    val qrPayload: String,
)

@JsonClass(generateAdapter = true)
data class PhoneLoginRequest(
    val phoneNumber: String,
)

@JsonClass(generateAdapter = true)
data class UidLoginRequest(
    val userId: String,
)

@JsonClass(generateAdapter = true)
data class QrLoginResponse(
    val token: String,
    val pushYn: String?,
    val memberId: String,
    val userId: Long,
    val name: String,
    val expiresAt: String?,
)
