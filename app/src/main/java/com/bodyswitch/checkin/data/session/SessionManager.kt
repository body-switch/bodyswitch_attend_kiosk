package com.bodyswitch.checkin.data.session

import com.bodyswitch.checkin.data.api.dto.BranchInfoResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val autoLoginManager: AutoLoginManager,
) {

    var token: String? = null
        private set
    var username: String? = null
        private set
    var name: String? = null
        private set
    var userRole: String? = null
        private set
    var branchId: Long? = null
        private set
    var branchName: String? = null
        private set
    var centerId: Long? = null
        private set
    var businessName: String? = null
        private set
    var centerType: String? = null
        private set

    // 지점 상세 정보
    var address: String? = null
        private set
    var addressDetail: String? = null
        private set
    var phone: String? = null
        private set

    val isLoggedIn: Boolean get() = token != null

    val bearerToken: String? get() = token?.let { "Bearer $it" }

    fun login(
        token: String,
        username: String,
        name: String?,
        userRole: String,
        branchId: Long?,
        branchName: String?,
        centerId: Long?,
        businessName: String?,
        centerType: String?,
    ) {
        this.token = token
        this.username = username
        this.name = name
        this.userRole = userRole
        this.branchId = branchId
        this.branchName = branchName
        this.centerId = centerId
        this.businessName = businessName
        this.centerType = centerType
    }

    fun updateBranchInfo(info: BranchInfoResponse) {
        branchName = info.branchName ?: branchName
        businessName = info.businessName ?: businessName
        address = info.address
        addressDetail = info.addressDetail
        phone = info.representativeNumber
    }

    fun logout() {
        token = null
        username = null
        name = null
        userRole = null
        branchId = null
        branchName = null
        centerId = null
        businessName = null
        centerType = null
        address = null
        addressDetail = null
        phone = null
        autoLoginManager.clear()
    }

    companion object {
        val ALLOWED_ROLES = setOf(
            "OPERATOR",
            "MANAGER",
            "EMPLOYEE",
            "GUEST",
        )
    }
}
