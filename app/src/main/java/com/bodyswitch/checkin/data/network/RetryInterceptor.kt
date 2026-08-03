package com.bodyswitch.checkin.data.network

import android.os.SystemClock
import android.util.Log
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 읽기 타임아웃이 났을 때 안전한 요청만 자동으로 한 번 더 시도한다.
 *
 * 현장에서 "실패했다가 다시 하면 된다"가 반복되는데, 그 재시도를 회원이 아니라 앱이 한다.
 * 시도당 타임아웃을 [ATTEMPT_TIMEOUT_SECONDS]초로 줄여 최대 2회를 돌아도 총 대기시간이
 * 기존 단일 시도(15초)를 넘지 않는다.
 *
 * **차감이 일어나는 요청은 절대 재시도하지 않는다.** 체크인 차감·출석·재입장을 재시도하면
 * 서버가 첫 요청을 이미 처리했을 때 횟수가 이중으로 빠진다. 응답을 못 받았을 뿐
 * 처리는 됐을 수 있기 때문이다. 조회(GET)와 토큰 발급(로그인)만 안전하다.
 */
@Singleton
class RetryInterceptor @Inject constructor(
    private val reporter: CheckinFailureReporter,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!isRetryable(request)) {
            return chain.proceed(request)
        }

        val scoped = chain.withReadTimeout(ATTEMPT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        val startedAt = SystemClock.elapsedRealtime()
        var lastError: IOException? = null

        for (attempt in 1..MAX_ATTEMPTS) {
            try {
                val response = scoped.proceed(request)
                if (attempt > 1) {
                    // 조용히 넘어가면 문제가 안 보이게 된다. 성공했어도 서버에 남긴다.
                    reporter.reportRetrySucceeded(
                        path = request.url.encodedPath,
                        attempt = attempt,
                        elapsedMs = SystemClock.elapsedRealtime() - startedAt,
                    )
                }
                return response
            } catch (e: SocketTimeoutException) {
                lastError = e
                Log.w(TAG, "읽기 타임아웃 - ${request.url.encodedPath} 시도 $attempt/$MAX_ATTEMPTS")
            }
        }

        throw lastError ?: IOException("재시도 실패: ${request.url.encodedPath}")
    }

    /**
     * 재시도해도 부작용이 없는 요청인지 판정한다.
     * GET은 조회뿐이라 안전하고, 로그인은 토큰 발급이라 중복 호출해도 상태가 바뀌지 않는다.
     */
    private fun isRetryable(request: Request): Boolean {
        if (request.method == "GET") {
            return true
        }
        return request.url.encodedPath.contains(LOGIN_PATH_MARKER)
    }

    private companion object {
        const val TAG = "CHECKIN"
        const val ATTEMPT_TIMEOUT_SECONDS = 7
        const val MAX_ATTEMPTS = 2
        const val LOGIN_PATH_MARKER = "/auth/"
    }
}
