package com.bodyswitch.checkin.ui.phone

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.bodyswitch.checkin.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val DarkBg = Color(0xFF000000)
private val GreenCard = Color(0x5255C982)
private val TealPrimary = Color(0xFF4AB3BC)
private val PanelBg = Color(0x14FFFFFF)
private val GrayText = Color(0xFFBFBFBF)
private val Red = Color(0xFFE53935)
private val KeyBg = Color(0xE0FFFFFF)          // rgba(255,255,255,0.88)
private val ActionKeyBg = Color(0xCC4AB3BC)     // rgba(74,179,188,0.80)
private val DotEmpty = Color(0xFF3A3A3A)
private val KeypadAreaBg = Color(0xFF1E1E1E)

@Composable
fun PhoneLoginScreen(
    onBack: () -> Unit,
    onLoginSuccess: (token: String) -> Unit,
    viewModel: PhoneLoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 실시간 시계
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val now = Date(currentTime)
    val calendar = Calendar.getInstance().apply { time = now }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.KOREA) }
    val timeFormat = remember { SimpleDateFormat("a hh:mm", Locale.KOREA) }
    val dayOfWeek = arrayOf("", "일", "월", "화", "수", "목", "금", "토")[calendar.get(Calendar.DAY_OF_WEEK)]

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.token) {
        uiState.token?.let { onLoginSuccess(it) }
    }

    // 8자리 입력 완료 시 자동 로그인
    LaunchedEffect(uiState.phoneNumber) {
        if (uiState.phoneNumber.length == 8 && !uiState.isLoading) {
            viewModel.login()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
    ) {
        // 배경 이미지
        Image(
            painter = painterResource(R.drawable.bg_diamond),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        // 메인 콘텐츠
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 100.dp),
        ) {
            // 왼쪽 - 번호 체크인 카드 + 안내
            Box(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // 그린 카드 헤더
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(GreenCard)
                            .padding(horizontal = 32.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = Color.White,
                            )
                        }

                        Spacer(modifier = Modifier.width(24.dp))

                        Column {
                            Text(
                                text = "번호 체크인",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "전화번호 뒤 8자리를 입력해 주세요",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GrayText,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "도움이 필요하시면 프론트에 문의해 주세요",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = TealPrimary,
                    )
                }
            }

            // 오른쪽 - 다크 패널에 입력 + 키패드
            Box(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
                    .padding(end = 48.dp, top = 24.dp, bottom = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // 8자리 입력 표시
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (i in 0 until 8) {
                            val char = uiState.phoneNumber.getOrNull(i)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(100.dp)
                                    .drawBehind {
                                        val strokeWidth = 4.dp.toPx()
                                        val color = Color.White.copy(alpha = 0.8f)
                                        drawLine(
                                            color = color,
                                            start = Offset(0f, size.height - strokeWidth / 2),
                                            end = Offset(size.width, size.height - strokeWidth / 2),
                                            strokeWidth = strokeWidth,
                                        )
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = char?.toString() ?: "",
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }
                        }
                    }

                    if (uiState.isLoading) {
                        Spacer(modifier = Modifier.height(24.dp))
                        CircularProgressIndicator(color = TealPrimary)
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // 숫자 키패드
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        KeypadRow(
                            keys = listOf("1", "2", "3"),
                            onKeyPress = { key -> appendDigit(key, uiState.phoneNumber, viewModel) },
                        )
                        KeypadRow(
                            keys = listOf("4", "5", "6"),
                            onKeyPress = { key -> appendDigit(key, uiState.phoneNumber, viewModel) },
                        )
                        KeypadRow(
                            keys = listOf("7", "8", "9"),
                            onKeyPress = { key -> appendDigit(key, uiState.phoneNumber, viewModel) },
                        )
                        // Clear, 0, 백스페이스
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // Clear
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(80.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(ActionKeyBg)
                                    .clickable { viewModel.onPhoneNumberChange("") },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Clear",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }
                            // 0
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(80.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(KeyBg)
                                    .clickable { appendDigit("0", uiState.phoneNumber, viewModel) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "0",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Black,
                                )
                            }
                            // ← 백스페이스
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(80.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(ActionKeyBg)
                                    .clickable {
                                        if (uiState.phoneNumber.isNotEmpty()) {
                                            viewModel.onPhoneNumberChange(
                                                uiState.phoneNumber.dropLast(1)
                                            )
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "←",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }
                        }
                        // 확인 버튼
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(
                                    if (uiState.phoneNumber.length == 8) ActionKeyBg
                                    else ActionKeyBg.copy(alpha = 0.4f)
                                )
                                .clickable {
                                    if (uiState.phoneNumber.length == 8) viewModel.login()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "확인",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }

        // 상단 바 (반투명)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PanelBg)
                .padding(horizontal = 48.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Row {
                    Text(
                        text = dateFormat.format(now),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        letterSpacing = (-0.2).sp,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${dayOfWeek})",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }
                Text(
                    text = timeFormat.format(now),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    letterSpacing = (-0.3).sp,
                )
            }

            Text(
                text = "바디스위치 피트니스",
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
            )

            Box(
                modifier = Modifier
                    .border(
                        width = 2.dp,
                        color = Color.White,
                        shape = RoundedCornerShape(999.dp),
                    )
                    .clickable { /* TODO: 직원호출 */ }
                    .padding(horizontal = 28.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "직원호출",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    letterSpacing = (-0.2).sp,
                )
            }
        }

        // 스낵바
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = Red,
                contentColor = Color.White,
            )
        }
    }
}

private fun appendDigit(
    digit: String,
    current: String,
    viewModel: PhoneLoginViewModel,
) {
    if (current.length < 8) {
        viewModel.onPhoneNumberChange(current + digit)
    }
}

@Composable
private fun KeypadRow(
    keys: List<String>,
    onKeyPress: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        keys.forEach { key ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(KeyBg)
                    .clickable { onKeyPress(key) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = key,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                )
            }
        }
    }
}

@Preview(
    name = "번호 체크인 화면",
    widthDp = 1920,
    heightDp = 1080,
    showBackground = true,
    backgroundColor = 0xFF000000,
)
@Composable
private fun PhoneLoginScreenPreview() {
    PhoneLoginScreen(
        onBack = {},
        onLoginSuccess = {},
    )
}
