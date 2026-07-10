package com.bodyswitch.checkin.ui.common

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * 현재 화면이 세로인지 여부.
 *
 * 앱은 기기의 자동회전 설정을 따르므로(Manifest `screenOrientation="user"`),
 * 각 화면은 이 값으로 가로/세로 배치를 분기한다.
 *
 * MainActivity에 `configChanges="orientation|screenSize|..."`가 걸려 있어
 * 회전해도 Activity 재생성 없이 리컴포즈만 일어난다.
 */
@Composable
@ReadOnlyComposable
fun isPortrait(): Boolean =
    LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
