package com.bodyswitch.checkin.data.api.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AdminLoginRequest(
    val username: String,
    val password: String,
)

@JsonClass(generateAdapter = true)
data class AdminLoginResponse(
    val token: String,
    val expiresAt: String?,
    val userRole: String,
    val username: String?,
    val name: String?,
    val branchId: Long?,
    val branchName: String?,
    val centerId: Long?,
    val businessName: String?,
    val centerType: String?,
)

@JsonClass(generateAdapter = true)
data class BranchInfoResponse(
    val branchId: Long?,
    val branchName: String?,
    val centerId: Long?,
    val businessName: String?,
    val centerType: String?,
    val address: String?,
    val addressDetail: String?,
    val representativeNumber: String?,
)
