package com.bodyswitch.checkin.data.api.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class QrLoginRequest(
    val qrPayload: String,
)

@JsonClass(generateAdapter = true)
data class PhoneLoginRequest(
    // 완전일치 조회. phoneLast4를 보내면 비운다.
    val phoneNumber: String? = null,
    // 뒤 4자리 조회. 후보가 2명 이상이면 서버가 409 + 후보 목록을 준다.
    val phoneLast4: String? = null,
    val allowCandidates: Boolean = false,
    // 후보 선택 후 재요청 시 지정
    val candidateId: String? = null,
)

/** 전화번호가 여러 명을 가리킬 때 본인 선택용 후보 (409 응답 본문) */
@JsonClass(generateAdapter = true)
data class MemberCandidate(
    // 재요청용 불투명 식별자. "MEMBER:{id}" 또는 "EMPLOYEE:{id}"
    val candidateId: String,
    val memberId: String? = null,
    val name: String,
    val maskedBirthDate: String,
    val maskedPhone: String? = null,
    val role: String? = null,
    // VALID / STOPPED / EXPIRED. 직원 후보는 null
    val passStatus: String? = null,
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
