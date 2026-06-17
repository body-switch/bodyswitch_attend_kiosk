package com.bodyswitch.checkin.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.bodyswitch.checkin.R
import kotlinx.coroutines.delay

private val TealColor = Color(0xFF4AB3BC)

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    // 0f → 1f 전체 진행도
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Phase 1: 로고가 위에서 아래로 내려오면서 커짐 (0 → 0.5)
        progress.animateTo(
            targetValue = 0.5f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        )
        delay(400)
        // Phase 2: 배경 틸→흰색, 로고 흰색→틸 (0.5 → 1.0)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        )
        delay(500)
        onSplashFinished()
    }

    val p = progress.value

    // Phase 1: 로고 이동 & 크기 (0 ~ 0.5)
    val movePhase = (p / 0.5f).coerceIn(0f, 1f)
    val logoOffsetY = androidx.compose.ui.unit.lerp((-30).dp, 0.dp, movePhase)
    val logoSize = androidx.compose.ui.unit.lerp(120.dp, 160.dp, movePhase)
    // Phase 2: 색상 전환 (0.5 ~ 1.0)
    val colorPhase = ((p - 0.5f) / 0.5f).coerceIn(0f, 1f)
    val bgColor = lerp(TealColor, Color.White, colorPhase)
    val tintColor = lerp(Color.White, TealColor, colorPhase)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_bodyswitch_full),
            contentDescription = null,
            colorFilter = ColorFilter.tint(tintColor),
            modifier = Modifier
                .size(logoSize)
                .offset(y = logoOffsetY),
        )
    }
}
