package com.bodyswitch.checkin.data.api.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class QrLoginRequest(
    val qrPayload: String,
)

@JsonClass(generateAdapter = true)
data class PhoneLoginRequest(
    val phoneNumber: String,
    // 출입등록만 true. 지점 내 후보가 2명 이상이면 서버가 409 + 후보 목록을 준다.
    // 체크인은 미전송(false)이라 기존대로 첫 후보 토큰을 받는다.
    val allowCandidates: Boolean = false,
    // 후보 선택 후 재요청 시 지정
    val memberId: String? = null,
)

/** 전화번호가 여러 회원을 가리킬 때 본인 선택용 후보 (409 응답 본문) */
@JsonClass(generateAdapter = true)
data class MemberCandidate(
    val memberId: String,
    val name: String,
    val maskedBirthDate: String,
)

@JsonClass(generateAdapter = true)
data class PhoneLoginCandidatesResponse(
    val candidates: List<MemberCandidate> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class UidLoginRequest(
    val userId: String,
)

@JsonClass(generateAdapter = true)
data class QrLoginResponse(
    val token: String,
    val pushYn: String?,
    val memberId: String?,
    val userId: Long,
    val name: String,
    val expiresAt: String?,
    val role: String? = null,
    val employeeId: Long? = null,
    val branchId: Long? = null,
)
