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
 *
 * 재발급이 안 됐는데 exp 가 이미 지났거나, 관리자 토큰을 실은 요청이 401 로 돌아오면
 * 세션을 비운다([SessionManager.expire]). 그대로 두면 "QR 코드가 유효하지 않습니다" 같은
 * 엉뚱한 에러만 반복돼 센터가 앱 버그로 오인한다.
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
            return expireOnUnauthorized(chain.proceed(request), inAuthHeader)
        }

        val newToken = refresher.refreshBlocking(oldToken)
        if (newToken == null || newToken == oldToken) {
            if (refresher.isExpired(oldToken)) {
                sessionManager.expire()
            }
            return expireOnUnauthorized(chain.proceed(request), inAuthHeader)
        }

        val builder = request.newBuilder()
        if (inAdminHeader) {
            builder.header(HEADER_ADMIN_TOKEN, newToken)
        }
        if (inAuthHeader) {
            builder.header(HEADER_AUTHORIZATION, "$BEARER_PREFIX$newToken")
        }
        return expireOnUnauthorized(chain.proceed(builder.build()), inAuthHeader)
    }

    /**
     * 관리자 토큰을 Bearer 로 실은 요청의 401 은 토큰이 서버에서 거부됐다는 뜻뿐이다
     * (만료·무효·계정 소실). 기기 시계가 틀려 로컬 exp 검사를 통과한 경우의 안전망.
     * `X-Admin-Token` 만 실은 요청은 401 이 회원 QR 불일치 등 다른 뜻이라 보지 않는다.
     */
    private fun expireOnUnauthorized(response: Response, inAuthHeader: Boolean): Response {
        if (inAuthHeader && response.code == HTTP_UNAUTHORIZED) {
            sessionManager.expire()
        }
        return response
    }

    companion object {
        private const val HEADER_ADMIN_TOKEN = "X-Admin-Token"
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
        private const val HTTP_UNAUTHORIZED = 401
    }
}
