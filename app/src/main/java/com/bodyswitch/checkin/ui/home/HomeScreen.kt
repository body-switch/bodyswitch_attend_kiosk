package com.bodyswitch.checkin.ui.home

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodyswitch.checkin.R
import com.bodyswitch.checkin.data.session.AutoLoginManager
import com.bodyswitch.checkin.data.session.CheckinSettingsManager
import com.bodyswitch.checkin.data.session.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val DarkBg = Color(0xFF000000)
private val DarkDrawer = Color(0xFF262626)
private val TealPrimary = Color(0xFF4AB3BC)
private val TealCard = Color(0x524AB3BC)   // rgba(74,179,188,0.32)
private val GreenCard = Color(0x5255C982)  // rgba(85,201,130,0.32)
private val PanelBg = Color(0x14FFFFFF)    // rgba(255,255,255,0.08)
private val GrayText = Color(0xFFBFBFBF)
private val Blue = Color(0xFF42A5F5)
private val BlueDark = Color(0xFF1565C0)
private val Red = Color(0xFFE53935)
private val Coral = Color(0xFFEE735A)
private val Teal = Color(0xFF4AB3BC)

@Composable
fun HomeScreen(
    sessionManager: SessionManager,
    checkinSettingsManager: CheckinSettingsManager? = null,
    onQrClick: () -> Unit,
    onPhoneClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit,
) {
    val qrEnabled = checkinSettingsManager?.qrCheckinEnabled ?: true
    val phoneEnabled = checkinSettingsManager?.phoneCheckinEnabled ?: true
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showStaffCallDialog by remember { mutableStateOf(false) }

    // 실시간 시계
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val now = Date(currentTime)
    val calendar = Calendar.getInstance().apply { time = now }
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
    val timeFormat = SimpleDateFormat("a hh:mm", Locale.KOREA)
    val dayOfWeek = arrayOf("", "일", "월", "화", "수", "목", "금", "토")[calendar.get(Calendar.DAY_OF_WEEK)]

    val roleDisplay = when (sessionManager.userRole) {
        "OPERATOR" -> "운영자"
        "MANAGER" -> "매니저"
        "EMPLOYEE" -> "직원"
        "GUEST" -> "게스트"
        else -> sessionManager.userRole ?: ""
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = DarkDrawer,
            ) {
                // 헤더: 관리자 정보
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .border(2.dp, TealPrimary, CircleShape)
                                .background(TealPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_drawer_person),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = sessionManager.name ?: sessionManager.username ?: "",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Text(
                            text = roleDisplay,
                            fontSize = 13.sp,
                            color = TealPrimary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 지점 정보
                Text(
                    text = "지점 정보",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    letterSpacing = 2.sp,
                )

                Spacer(modifier = Modifier.height(8.dp))

                DrawerInfoRow(
                    painter = painterResource(R.drawable.ic_drawer_store),
                    iconSize = 20.dp,
                    label = "지점명",
                    value = sessionManager.branchName ?: "-",
                )

                sessionManager.address?.let { addr ->
                    val full = if (sessionManager.addressDetail != null) {
                        "$addr ${sessionManager.addressDetail}"
                    } else addr
                    DrawerInfoRow(
                        painter = painterResource(R.drawable.ic_drawer_location),
                        iconSize = 18.dp,
                        label = "주소",
                        value = full,
                    )
                }

                sessionManager.phone?.let { phone ->
                    DrawerInfoRow(
                        painter = painterResource(R.drawable.ic_drawer_phone),
                        iconSize = 18.dp,
                        label = "전화",
                        value = phone,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                // 출입 기록
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch { drawerState.close() }
                            onHistoryClick()
                        }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_drawer_history),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("출입 기록", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }

                // 메인 설정
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch { drawerState.close() }
                            onSettingsClick()
                        }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_drawer_settings),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("메인 설정", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.weight(1f))

                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                TextButton(
                    onClick = {
                        scope.launch { drawerState.close() }
                        onLogout()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_drawer_logout),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("로그아웃", color = Coral, fontWeight = FontWeight.Bold)
                }
            }
        },
    ) {
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
                // 왼쪽 영역 - 로고 + 체크인 안내
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
                        Image(
                            painter = painterResource(R.drawable.logo_bodyswitch_full),
                            contentDescription = "BODYSWITCH 체크인",
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .padding(horizontal = 16.dp),
                            contentScale = ContentScale.FillWidth,
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "인증 방식을 선택해 주세요",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            color = GrayText,
                        )
                    }
                }

                // 오른쪽 영역 - 체크인 카드
                Column(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // QR 체크인 카드
                    if (qrEnabled) {
                        CheckinMethodCard(
                            painter = painterResource(id = R.drawable.ic_qr_check),
                            title = "QR 체크인",
                            subtitle = "앱의 QR코드를 스캐너에 비춰주세요",
                            backgroundColor = TealCard,
                            onClick = onQrClick,
                        )
                    }

                    if (qrEnabled && phoneEnabled) {
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // 번호 체크인 카드
                    if (phoneEnabled) {
                        CheckinMethodCard(
                            painter = painterResource(id = R.drawable.ic_phone_check),
                            title = "번호 체크인",
                            subtitle = "전화번호 뒤 8자리를 입력해 주세요",
                            backgroundColor = GreenCard,
                            onClick = onPhoneClick,
                        )
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
                // 날짜/시간
                Column(
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { scope.launch { drawerState.open() } },
                ) {
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

                // 센터명
                Text(
                    text = sessionManager.businessName ?: sessionManager.branchName ?: "",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                )

                // 직원호출 버튼
                Box(
                    modifier = Modifier
                        .border(
                            width = 2.dp,
                            color = Color.White,
                            shape = RoundedCornerShape(999.dp),
                        )
                        .clickable { showStaffCallDialog = true }
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
        }
    }

    // 직원호출 다이얼로그
    if (showStaffCallDialog) {
        StaffCallDialog(
            onDismiss = { showStaffCallDialog = false },
            onConfirm = {
                showStaffCallDialog = false
                // TODO: 직원 호출 API 연동
            },
        )
    }
}

@Composable
private fun StaffCallDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(top = 40.dp, bottom = 24.dp, start = 32.dp, end = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "직원을 호출하시겠습니까?",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "직원 호출 요청 시, 담당자에게 알림이 발송됩니다",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(32.dp))

            HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp)

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 닫기 버튼
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .border(
                            width = 1.dp,
                            color = Color(0xFFD9D9D9),
                            shape = RoundedCornerShape(8.dp),
                        )
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "닫기",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray,
                    )
                }

                // 호출 버튼
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(TealPrimary)
                        .clickable { onConfirm() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "호출",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerInfoRow(
    painter: Painter,
    iconSize: Dp = 18.dp,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, fontSize = 11.sp, color = Color.Gray)
            Text(text = value, fontSize = 14.sp, color = Color.White)
        }
    }
}

@Composable
private fun CheckinMethodCard(
    painter: Painter,
    title: String,
    subtitle: String,
    backgroundColor: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .width(500.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(36.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 아이콘 영역
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
            )
        }

        Spacer(modifier = Modifier.width(32.dp))

        // 텍스트 영역
        Column {
            Text(
                text = title,
                fontSize = 40.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = GrayText,
            )
        }
    }
}

@Preview(
    name = "홈 화면",
    widthDp = 1920,
    heightDp = 1080,
    showBackground = true,
    backgroundColor = 0xFF000000,
)
@Composable
private fun HomeScreenPreview() {
    val sessionManager = SessionManager(AutoLoginManager(androidx.compose.ui.platform.LocalContext.current))
    sessionManager.login(
        token = "preview",
        username = "admin",
        name = "홍길동",
        userRole = "MANAGER",
        branchId = 1L,
        branchName = "강남점",
        centerId = 1L,
        businessName = "바디스위치 피트니스",
        centerType = "FITNESS",
    )
    HomeScreen(
        sessionManager = sessionManager,
        onQrClick = {},
        onPhoneClick = {},
        onHistoryClick = {},
        onLogout = {},
    )
}
