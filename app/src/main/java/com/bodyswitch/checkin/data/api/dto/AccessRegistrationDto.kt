package com.bodyswitch.checkin.data.api.dto

import com.squareup.moshi.JsonClass

// 안면등록 요청. authImage는 base64 인코딩 JPEG.
@JsonClass(generateAdapter = true)
data class FaceRegistrationRequest(
    val branchId: Long,
    val authImage: String,
)

// 안면등록 응답. accessGranted=false면 출입권 미등록(데스크 문의 안내).
@JsonClass(generateAdapter = true)
data class FaceRegistrationResponse(
    val accessGranted: Boolean = false,
    val message: String? = null,
)

// QR 발급 요청
@JsonClass(generateAdapter = true)
data class QrIssuanceRequest(
    val branchId: Long,
)

// QR 발급 응답. qrPayload를 QR 코드로 렌더링.
@JsonClass(generateAdapter = true)
data class QrIssuanceResponse(
    val qrPayload: String,
    val accessGranted: Boolean = false,
    val message: String? = null,
)
