package com.bodyswitch.checkin.data.session

import android.content.Context
import android.content.SharedPreferences
import com.bodyswitch.checkin.data.api.dto.BranchInfoResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 로그인 세션을 보관한다.
 *
 * 값을 **SharedPreferences에 영속화한다.** 예전에는 in-memory `var`로만 들고 있어서
 * 안드로이드가 앱 프로세스를 회수하면 토큰 나이와 무관하게 세션이 통째로 사라졌고,
 * 키오스크를 계속 켜뒀는데도 로그인 화면으로 돌아가는 증상이 있었다.
 *
 * 토큰 자체의 만료(7일)는 여기서 다루지 않는다. 그건 [AdminTokenRefresher]의 몫이다.
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext context: Context,
    private val autoLoginManager: AutoLoginManager,
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var token: String? = prefs.getString(KEY_TOKEN, null)
        private set
    var username: String? = prefs.getString(KEY_USERNAME, null)
        private set
    var name: String? = prefs.getString(KEY_NAME, null)
        private set
    var userRole: String? = prefs.getString(KEY_USER_ROLE, null)
        private set
    var branchId: Long? = prefs.getLongOrNull(KEY_BRANCH_ID)
        private set
    var branchName: String? = prefs.getString(KEY_BRANCH_NAME, null)
        private set
    var centerId: Long? = prefs.getLongOrNull(KEY_CENTER_ID)
        private set
    var businessName: String? = prefs.getString(KEY_BUSINESS_NAME, null)
        private set
    var centerType: String? = prefs.getString(KEY_CENTER_TYPE, null)
        private set

    // 지점 상세 정보
    var address: String? = prefs.getString(KEY_ADDRESS, null)
        private set
    var addressDetail: String? = prefs.getString(KEY_ADDRESS_DETAIL, null)
        private set
    var phone: String? = prefs.getString(KEY_PHONE, null)
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

        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USERNAME, username)
            .putString(KEY_NAME, name)
            .putString(KEY_USER_ROLE, userRole)
            .putLongOrRemove(KEY_BRANCH_ID, branchId)
            .putString(KEY_BRANCH_NAME, branchName)
            .putLongOrRemove(KEY_CENTER_ID, centerId)
            .putString(KEY_BUSINESS_NAME, businessName)
            .putString(KEY_CENTER_TYPE, centerType)
            .apply()
    }

    fun updateBranchInfo(info: BranchInfoResponse) {
        branchName = info.branchName ?: branchName
        businessName = info.businessName ?: businessName
        address = info.address
        addressDetail = info.addressDetail
        phone = info.representativeNumber

        prefs.edit()
            .putString(KEY_BRANCH_NAME, branchName)
            .putString(KEY_BUSINESS_NAME, businessName)
            .putString(KEY_ADDRESS, address)
            .putString(KEY_ADDRESS_DETAIL, addressDetail)
            .putString(KEY_PHONE, phone)
            .apply()
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

        prefs.edit().clear().apply()
        autoLoginManager.clear()
    }

    companion object {
        private const val PREFS_NAME = "session"
        private const val KEY_TOKEN = "token"
        private const val KEY_USERNAME = "username"
        private const val KEY_NAME = "name"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_BRANCH_ID = "branch_id"
        private const val KEY_BRANCH_NAME = "branch_name"
        private const val KEY_CENTER_ID = "center_id"
        private const val KEY_BUSINESS_NAME = "business_name"
        private const val KEY_CENTER_TYPE = "center_type"
        private const val KEY_ADDRESS = "address"
        private const val KEY_ADDRESS_DETAIL = "address_detail"
        private const val KEY_PHONE = "phone"

        val ALLOWED_ROLES = setOf(
            "OPERATOR",
            "MANAGER",
            "EMPLOYEE",
            "GUEST",
        )
    }
}

/** 값이 없으면 null. SharedPreferences에는 nullable Long이 없어 존재 여부로 구분한다. */
private fun SharedPreferences.getLongOrNull(key: String): Long? =
    if (contains(key)) getLong(key, 0L) else null

private fun SharedPreferences.Editor.putLongOrRemove(
    key: String,
    value: Long?,
): SharedPreferences.Editor = if (value != null) putLong(key, value) else remove(key)
