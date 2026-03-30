package com.bodyswitch.checkin.ui.checkin

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodyswitch.checkin.R
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val TealPrimary = Color(0xFF4AB3BC)
private val TealButton = Color(0xFF3E9FA7)
private val ScreenDarkBg = Color(0xFF000000)
private val ScreenGrayText = Color(0xFFA6A6A6)

@Composable
fun CheckinCompleteScreen(
    onScanAgain: () -> Unit,
    centerName: String = "",
) {
    var countdown by remember { mutableIntStateOf(3) }
    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.3f,
        animationSpec = tween(500),
        label = "scale",
    )

    // 실시간 시계
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        visible = true
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

    // 3초 카운트다운 후 자동 이동
    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000L)
            countdown--
        }
        onScanAgain()
    }

    val now = Date(currentTime)
    val calendar = Calendar.getInstance().apply { time = now }
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
    val timeFormat = SimpleDateFormat("a hh:mm", Locale.KOREA)
    val dayOfWeek = arrayOf("", "일", "월", "화", "수", "목", "금", "토")[calendar.get(Calendar.DAY_OF_WEEK)]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenDarkBg),
    ) {
        // 배경 이미지
        Image(
            painter = painterResource(R.drawable.bg_diamond),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        // 상단 바
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.08f))
                .align(Alignment.TopCenter),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            dateFormat.format(now),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            letterSpacing = (-0.2).sp,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "($dayOfWeek)",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                    Text(
                        timeFormat.format(now),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        letterSpacing = (-0.3).sp,
                    )
                }
                Text(
                    text = centerName,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                )
                Box(
                    modifier = Modifier
                        .border(2.dp, Color.White, RoundedCornerShape(999.dp))
                        .padding(horizontal = 28.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("직원호출", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Color.White)
                }
            }
        }

        // 중앙 콘텐츠
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 120.dp, bottom = 120.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // 대형 체크 아이콘
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(TealPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(180.dp),
                    tint = Color.White,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "체크인 완료",
                fontSize = 50.sp,
                fontWeight = FontWeight.SemiBold,
                color = TealPrimary,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${countdown}초 뒤에 자동으로 창이 닫힙니다",
                fontSize = 32.sp,
                fontWeight = FontWeight.Medium,
                color = ScreenGrayText,
            )
        }

        // 하단 고정 버튼
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.625f)
                    .height(100.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(TealButton)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { onScanAgain() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "메인으로 이동",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }
    }
}
