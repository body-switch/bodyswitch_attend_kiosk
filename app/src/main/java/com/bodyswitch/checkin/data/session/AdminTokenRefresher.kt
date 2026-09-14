package com.bodyswitch.checkin.data.session

import android.util.Base64
import android.util.Log
import com.bodyswitch.checkin.data.api.KioskApi
import com.bodyswitch.checkin.data.api.dto.AdminLoginRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * 관리자 토큰(JWT) 만료를 선제 감지하고 저장된 자동로그인 자격증명으로 재발급한다.
 *
 * 키오스크 태블릿을 장시간 켜두면 앱이 재시작되지 않아 [SessionManager.token]이
 * 서버 만료(7일) 이후에도 그대로 남고, 안면등록/QR체크인 등에서
 * "지점 정보를 확인할 수 없습니다"가 발생한다. 만료 임박 시 조용히 재로그인해 이를 막는다.
 *
 * 재로그인이 안 되는 경우(자격증명 미저장, 서버 거절, 네트워크)에는 만료가 지난 토큰을
 * 계속 들고 있어봐야 요청마다 실패만 나온다. [ensureValidOrExpire]가 그 시점에 세션을 비워
 * 로그인 화면으로 보낸다.
 */
@Singleton
class AdminTokenRefresher @Inject constructor(
    @Named("refresh") private val api: KioskApi,
    private val sessionManager: SessionManager,
    private val autoLoginManager: AutoLoginManager,
) {
    private val lock = Any()

    /** 토큰이 없거나 만료 임박(버퍼 이내)/파싱 불가면 갱신 대상이다. */
    fun isNearExpiry(token: String?): Boolean {
        if (token == null) return true
        val exp = decodeExpEpochSeconds(token) ?: return true
        val now = System.currentTimeMillis() / MILLIS_PER_SECOND
        return now >= exp - EXPIRY_BUFFER_SECONDS
    }

    /** exp 가 이미 지났거나 파싱 불가면 만료로 본다. 토큰이 없으면 false — 로그아웃 상태는 만료가 아니다. */
    fun isExpired(token: String?): Boolean {
        if (token == null) return false
        val exp = decodeExpEpochSeconds(token) ?: return true
        return System.currentTimeMillis() / MILLIS_PER_SECOND >= exp
    }

    /**
     * 현재 토큰이 만료됐으면 재로그인을 시도하고, 그래도 만료 상태면 세션을 비운다.
     * 홈 화면 진입과 주기 검사에서 호출한다. 요청이 나가지 않는 상주 태블릿도 여기서 잡힌다.
     */
    suspend fun ensureValidOrExpire() {
        val current = sessionManager.token ?: return
        if (!isExpired(current)) return
        val refreshed = withContext(Dispatchers.IO) { refreshBlocking(current) }
        if (isExpired(refreshed)) {
            Log.w(TAG, "관리자 토큰 만료 확정 - 세션 비움")
            sessionManager.expire()
        }
    }

    /**
     * 저장된 자동로그인 자격증명으로 동기 재로그인 후 새 토큰을 반환한다.
     * 다른 요청이 이미 갱신했거나 자격증명이 없거나 실패하면 현재 토큰을 그대로 반환한다.
     * OkHttp 인터셉터의 백그라운드 스레드에서 호출되므로 blocking을 허용한다.
     */
    fun refreshBlocking(oldToken: String?): String? {
        synchronized(lock) {
            val current = sessionManager.token
            // 다른 요청이 먼저 갱신했으면 재사용한다.
            if (current != null && current != oldToken && !isNearExpiry(current)) {
                return current
            }
            if (!autoLoginManager.isEnabled) return current
            val username = autoLoginManager.username ?: return current
            val password = autoLoginManager.password ?: return current
            return try {
                val response = runBlocking {
                    api.adminLogin(AdminLoginRequest(username = username, password = password))
                }
                sessionManager.login(
                    token = response.token,
                    username = response.username ?: username,
                    name = response.name,
                    userRole = response.userRole,
                    branchId = response.branchId,
                    branchName = response.branchName,
                    centerId = response.centerId,
                    businessName = response.businessName,
                    centerType = response.centerType,
                )
                Log.i(TAG, "관리자 토큰 자동 갱신 성공")
                response.token
            } catch (e: Exception) {
                Log.e(TAG, "관리자 토큰 자동 갱신 실패", e)
                current
            }
        }
    }

    private fun decodeExpEpochSeconds(token: String): Long? {
        val parts = token.split(".")
        if (parts.size < 2) return null
        return try {
            val payload = Base64.decode(
                parts[1],
                Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
            )
            val exp = JSONObject(String(payload, Charsets.UTF_8)).optLong("exp", 0L)
            if (exp > 0L) exp else null
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val TAG = "ADMIN_TOKEN"
        private const val MILLIS_PER_SECOND = 1000L

        // 만료 1일 전부터 선제 갱신한다.
        private const val EXPIRY_BUFFER_SECONDS = 24 * 60 * 60L
    }
}
