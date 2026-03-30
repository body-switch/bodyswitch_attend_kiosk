package com.bodyswitch.checkin.data.model

data class ScanResult(
    val rawValue: String,
    val memberId: String?,
    val timestamp: Long = System.currentTimeMillis(),
)
