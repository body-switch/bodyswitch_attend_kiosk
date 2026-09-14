package com.bodyswitch.checkin.data.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 앱이 떠 있는 동안 관리자 토큰 만료를 주기적으로 검사한다.
 *
 * 상주 태블릿은 회원이 QR 을 찍기 전까지 요청이 한 건도 안 나가므로 인터셉터만으로는
 * 만료를 알 수 없다. 홈 진입 시와 [CHECK_INTERVAL_MS] 마다 검사해 만료면 세션을 비우고,
 * [expired] 로 화면을 로그인으로 보낸다.
 */
@HiltViewModel
class SessionExpiryViewModel @Inject constructor(
    sessionManager: SessionManager,
    private val refresher: AdminTokenRefresher,
) : ViewModel() {

    val expired: SharedFlow<Unit> = sessionManager.sessionExpired

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(CHECK_INTERVAL_MS)
                refresher.ensureValidOrExpire()
            }
        }
    }

    fun checkNow() {
        viewModelScope.launch { refresher.ensureValidOrExpire() }
    }

    private companion object {
        const val CHECK_INTERVAL_MS = 10 * 60 * 1000L
    }
}
