package com.bodyswitch.checkin.data.network

import android.util.Log
import com.bodyswitch.checkin.data.api.KioskApi
import com.bodyswitch.checkin.data.api.dto.CheckinFailureReportRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * 체크인 실패를 서버로 보고해 운영 서버 로그(`[KIOSK-CLIENT]`)에 남긴다.
 *
 * 인터셉터가 달리지 않은 `@Named("refresh")` 클라이언트를 쓴다. 보고 요청이
 * 관리자 토큰 갱신을 유발하거나 인터셉터에 재진입하지 않게 하기 위해서다.
 *
 * 전송 실패는 삼킨다. 보고가 실패했다고 원래 에러 화면을 덮으면 안 된다.
 * 회선이 완전히 끊긴 경우는 보고도 같은 회선을 타므로 서버에 남지 않는다 — 알려진 한계다.
 */
@Singleton
class CheckinFailureReporter @Inject constructor(
    @Named("refresh") private val api: KioskApi,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun report(
        step: Step,
        failure: CheckinFailure,
        elapsedMs: Long,
        branchId: Long?,
        memberName: String?,
        checkInMethod: String?,
    ) {
        scope.launch {
            runCatching {
                api.reportCheckinFailure(
                    CheckinFailureReportRequest(
                        step = step.name,
                        branchId = branchId,
                        memberName = memberName,
                        checkInMethod = checkInMethod,
                        httpStatus = failure.httpStatus,
                        errorType = failure.errorType,
                        errorMessage = failure.errorMessage,
                        errorBody = failure.errorBody,
                        elapsedMs = elapsedMs,
                    )
                )
            }.onFailure { Log.w(TAG, "실패 보고 전송 실패", it) }
        }
    }

    /** 실패가 발생한 체크인 단계. 서버 로그의 step= 값이 된다. */
    enum class Step {
        QR_LOGIN,
        TICKETS,
        RESERVATIONS,
        ATTEND,
        REENTRY,
        CHECKIN,
        OPEN_DOOR,
    }

    private companion object {
        const val TAG = "CHECKIN"
    }
}
