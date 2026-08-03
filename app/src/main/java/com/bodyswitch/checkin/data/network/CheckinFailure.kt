package com.bodyswitch.checkin.data.network

import com.bodyswitch.checkin.data.api.dto.ErrorResponse
import com.squareup.moshi.Moshi
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * 실패 원인을 화면 문구와 서버 보고용 필드로 분해한 결과.
 *
 * 지금까지는 모든 실패가 catch (e: Exception) 하나로 뭉개져 "무엇이 실패했는지"가
 * 화면에도 서버에도 남지 않았다. 이 타입이 그 분해를 한 곳에서 담당한다.
 */
data class CheckinFailure(
    val userMessage: String,
    val httpStatus: Int?,
    val errorType: String,
    val errorMessage: String?,
    val errorBody: String?,
)

/**
 * 예외를 [CheckinFailure]로 분류한다.
 *
 * @param fallback 서버가 사유를 주지 않았을 때 쓸 단계별 기본 문구
 * @param isOnline 단말이 인터넷에 연결돼 있는지. 회선 문제와 서버 문제를 구분해 안내한다
 */
fun Throwable.toCheckinFailure(
    moshi: Moshi,
    isOnline: Boolean,
    fallback: String,
): CheckinFailure = when (this) {
    is HttpException -> {
        // errorBody 스트림은 한 번만 읽을 수 있으므로 여기서 읽어 두고 재사용한다
        val body = runCatching { response()?.errorBody()?.string() }.getOrNull()
        val serverMessage = runCatching {
            moshi.adapter(ErrorResponse::class.java).fromJson(body.orEmpty())?.message
        }.getOrNull()
        CheckinFailure(
            userMessage = serverMessage ?: "$fallback (${code()})",
            httpStatus = code(),
            errorType = "HttpException",
            errorMessage = message(),
            errorBody = body,
        )
    }

    is SocketTimeoutException -> CheckinFailure(
        userMessage = "서버 응답이 지연됩니다. 잠시 후 다시 시도해 주세요",
        httpStatus = null,
        errorType = "SocketTimeoutException",
        errorMessage = message,
        errorBody = null,
    )

    is IOException -> CheckinFailure(
        userMessage = if (isOnline) "서버에 연결할 수 없습니다" else "인터넷 연결이 필요합니다",
        httpStatus = null,
        errorType = this::class.java.simpleName,
        errorMessage = message,
        errorBody = null,
    )

    else -> CheckinFailure(
        userMessage = fallback,
        httpStatus = null,
        errorType = this::class.java.simpleName,
        errorMessage = message,
        errorBody = null,
    )
}
