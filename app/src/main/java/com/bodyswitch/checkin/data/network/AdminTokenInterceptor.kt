package com.bodyswitch.checkin.data.network

import com.bodyswitch.checkin.data.session.AdminTokenRefresher
import com.bodyswitch.checkin.data.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 요청이 관리자 토큰을 실어보내고 그 토큰이 만료 임박이면
 * 저장된 자동로그인 자격증명으로 선제 재발급한 뒤 헤더를 교체한다.
 *
 * 관리자 토큰은 [SessionManager.token]에 있으며 요청에서 `X-Admin-Token` 또는
 * `Authorization: Bearer <token>`로 전달된다. 두 헤더 모두 처리한다.
 */
@Singleton
class AdminTokenInterceptor @Inject constructor(
    private val sessionManager: SessionManager,
    private val refresher: AdminTokenRefresher,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val oldToken = sessionManager.token ?: return chain.proceed(request)

        val inAdminHeader = request.header(HEADER_ADMIN_TOKEN) == oldToken
        val inAuthHeader = request.header(HEADER_AUTHORIZATION) == "$BEARER_PREFIX$oldToken"
        if (!inAdminHeader && !inAuthHeader) {
            return chain.proceed(request)
        }
        if (!refresher.isNearExpiry(oldToken)) {
            return chain.proceed(request)
        }

        val newToken = refresher.refreshBlocking(oldToken)
        if (newToken == null || newToken == oldToken) {
            return chain.proceed(request)
        }

        val builder = request.newBuilder()
        if (inAdminHeader) {
            builder.header(HEADER_ADMIN_TOKEN, newToken)
        }
        if (inAuthHeader) {
            builder.header(HEADER_AUTHORIZATION, "$BEARER_PREFIX$newToken")
        }
        return chain.proceed(builder.build())
    }

    companion object {
        private const val HEADER_ADMIN_TOKEN = "X-Admin-Token"
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }
}
