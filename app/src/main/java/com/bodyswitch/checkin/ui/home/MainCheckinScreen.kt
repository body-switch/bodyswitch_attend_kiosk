package com.bodyswitch.checkin.ui.home

import android.Manifest
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.bodyswitch.checkin.R
import com.bodyswitch.checkin.data.session.CheckinSettingsManager
import com.bodyswitch.checkin.data.session.SessionManager
import com.bodyswitch.checkin.ui.phone.PhoneLoginViewModel
import com.bodyswitch.checkin.ui.scanner.CameraPreview
import com.bodyswitch.checkin.ui.scanner.ScannerViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val DarkBg = Color(0xFF000000)
private val DarkDrawer = Color(0xFF000000)
private val TealPrimary = Color(0xFF4AB3BC)
private val TealCard = Color(0x524AB3BC)
private val GreenCard = Color(0x5255C982)
private val PanelBg = Color(0x14FFFFFF)
private val GrayText = Color(0xFFBFBFBF)
private val CameraAreaBg = Color(0xFF1E1E1E)
private val KeyBg = Color(0xE0FFFFFF)
private val ActionKeyBg = Color(0xCC4AB3BC)
private val DotEmpty = Color(0xFF3A3A3A)
private val KeypadAreaBg = Color(0xFF1E1E1E)
private val Red = Color(0xFFE53935)
private val Coral = Color(0xFFEE735A)
private val SidebarBg = Color(0x33000000)

private enum class CheckinMode { QR, PHONE, BOTH }

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainCheckinScreen(
    sessionManager: SessionManager,
    checkinSettingsManager: CheckinSettingsManager,
    onQrScanned: (String) -> Unit,
    onPhoneLogin: (String) -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogout: () -> Unit,
    scannerViewModel: ScannerViewModel = hiltViewModel(),
    phoneViewModel: PhoneLoginViewModel = hiltViewModel(),
    staffCallViewModel: StaffCallViewModel = hiltViewModel(),
) {
    val qrEnabled = checkinSettingsManager.qrCheckinEnabled
    val phoneEnabled = checkinSettingsManager.phoneCheckinEnabled

    val defaultMode = when {
        qrEnabled && phoneEnabled -> CheckinMode.BOTH
        qrEnabled -> CheckinMode.QR
        else -> CheckinMode.PHONE
    }

    var currentMode by remember { mutableStateOf(defaultMode) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarIsSuccess by remember { mutableStateOf(false) }
    var showStaffCallDialog by remember { mutableStateOf(false) }

    val staffCallState by staffCallViewModel.state.collectAsState()

    LaunchedEffect(staffCallState) {
        when (val s = staffCallState) {
            is StaffCallState.Success -> {
                snackbarIsSuccess = true
                snackbarHostState.showSnackbar(s.message)
                staffCallViewModel.resetState()
            }
            is StaffCallState.Error -> {
                snackbarIsSuccess = false
                snackbarHostState.showSnackbar(s.message)
                staffCallViewModel.resetState()
            }
            else -> Unit
        }
    }

    val scannerUiState by scannerViewModel.uiState.collectAsState()
    val phoneUiState by phoneViewModel.uiState.collectAsState()

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    // 카메라 권한 자동 요청
    LaunchedEffect(Unit) {
        if (qrEnabled && !cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    // 에러 처리
    LaunchedEffect(scannerUiState.errorMessage) {
        scannerUiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            scannerViewModel.clearError()
        }
    }
    LaunchedEffect(phoneUiState.error) {
        phoneUiState.error?.let {
            snackbarHostState.showSnackbar(it)
            phoneViewModel.clearError()
        }
    }
    LaunchedEffect(phoneUiState.token) {
        phoneUiState.token?.let {
            onPhoneLogin(it)
            phoneViewModel.clearToken()
        }
    }

    // 8자리 자동 로그인 (phoneNumber가 8자리로 변경되는 시점에만 실행)
    LaunchedEffect(phoneUiState.phoneNumber) {
        if (phoneUiState.phoneNumber.length == 8 && !phoneUiState.isLoading && !phoneUiState.loginDispatched) {
            phoneViewModel.login()
        }
    }

    // 설정 변경 시 모드 업데이트
    LaunchedEffect(qrEnabled, phoneEnabled) {
        currentMode = when {
            qrEnabled && phoneEnabled -> CheckinMode.BOTH
            qrEnabled -> CheckinMode.QR
            else -> CheckinMode.PHONE
        }
    }

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
            DrawerContent(
                sessionManager = sessionManager,
                roleDisplay = roleDisplay,
                scope = scope,
                drawerState = drawerState,
                onHistoryClick = onHistoryClick,
                onSettingsClick = onSettingsClick,
                onLogout = onLogout,
            )
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 140.dp),
            ) {
                // 체크인 영역
                when (currentMode) {
                    CheckinMode.BOTH -> BothCheckinContent(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        cameraPermission = cameraPermission.status.isGranted,
                        onRequestPermission = { cameraPermission.launchPermissionRequest() },
                        onQrDetected = { scannerViewModel.onQrDetected(it, onQrScanned) },
                        phoneNumber = phoneUiState.phoneNumber,
                        isPhoneLoading = phoneUiState.isLoading,
                        onDigit = { d ->
                            if (phoneUiState.phoneNumber.length < 8)
                                phoneViewModel.onPhoneNumberChange(phoneUiState.phoneNumber + d)
                        },
                        onDelete = {
                            if (phoneUiState.phoneNumber.isNotEmpty())
                                phoneViewModel.onPhoneNumberChange(phoneUiState.phoneNumber.dropLast(1))
                        },
                        onClear = { phoneViewModel.onPhoneNumberChange("") },
                    )
                    CheckinMode.QR -> QrOnlyContent(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        cameraPermission = cameraPermission.status.isGranted,
                        onRequestPermission = { cameraPermission.launchPermissionRequest() },
                        onQrDetected = { scannerViewModel.onQrDetected(it, onQrScanned) },
                    )
                    CheckinMode.PHONE -> PhoneOnlyContent(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        phoneNumber = phoneUiState.phoneNumber,
                        isLoading = phoneUiState.isLoading,
                        onDigit = { d ->
                            if (phoneUiState.phoneNumber.length < 8)
                                phoneViewModel.onPhoneNumberChange(phoneUiState.phoneNumber + d)
                        },
                        onDelete = {
                            if (phoneUiState.phoneNumber.isNotEmpty())
                                phoneViewModel.onPhoneNumberChange(phoneUiState.phoneNumber.dropLast(1))
                        },
                        onClear = { phoneViewModel.onPhoneNumberChange("") },
                    )
                }
            }

            // 상단 바
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                TopBar(
                    centerName = sessionManager.businessName ?: sessionManager.branchName ?: "",
                    onStaffCall = { showStaffCallDialog = true },
                    onDateClick = { scope.launch { drawerState.open() } },
                    switchText = when (currentMode) {
                        CheckinMode.QR -> "전화 체크인"
                        CheckinMode.PHONE -> "QR 체크인"
                        CheckinMode.BOTH -> ""
                    },
                    onSwitch = {
                        currentMode = when (currentMode) {
                            CheckinMode.QR -> CheckinMode.PHONE
                            CheckinMode.PHONE -> CheckinMode.QR
                            CheckinMode.BOTH -> CheckinMode.BOTH
                        }
                    },
                )
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
                    containerColor = if (snackbarIsSuccess) TealPrimary else Red,
                    contentColor = Color.White,
                )
            }
        }
    }

    if (showStaffCallDialog) {
        StaffCallDialog(
            onDismiss = { showStaffCallDialog = false },
            onConfirm = {
                showStaffCallDialog = false
                staffCallViewModel.callStaff(checkinSettingsManager.staffPhoneNumber)
            },
        )
    }
}

// ─── 둘 다 표시 (분할) ───
@Composable
private fun BothCheckinContent(
    modifier: Modifier,
    cameraPermission: Boolean,
    onRequestPermission: () -> Unit,
    onQrDetected: (String) -> Unit,
    phoneNumber: String,
    isPhoneLoading: Boolean,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp),
    ) {
        // ── 광고 영역 (공통) ──
        AdColumn(modifier = Modifier.weight(0.3f).fillMaxHeight())

        Spacer(modifier = Modifier.width(16.dp))

        // ── QR 영역 (680/1920 비율, 가장 넓음) ──
        Column(
            modifier = Modifier
                .weight(0.38f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // QR 헤더
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_qr_check),
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("QR 체크인", fontSize = 35.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("앱의 QR코드를 스캔해 주세요", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = GrayText)
                }
            }

            // 카메라 (rounded-32, #404040)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xFF404040)),
                contentAlignment = Alignment.Center,
            ) {
                if (cameraPermission) {
                    CameraPreview(
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(32.dp)),
                        onQrDetected = onQrDetected,
                    )
                } else {
                    CameraPermissionPlaceholder(onRequestPermission)
                }
                QrScanOverlay(scanSize = 350.dp)
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // ── 번호 영역 (568/1920 비율) ──
        Column(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // 번호 헤더
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_phone_check),
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("번호 체크인", fontSize = 35.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("전화번호 뒤 8자리를 입력해 주세요", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = GrayText)
                }
            }

            // 입력바 + 키패드
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PhoneKeypadContent(
                    phoneNumber = phoneNumber,
                    isLoading = isPhoneLoading,
                    onDigit = onDigit,
                    onDelete = onDelete,
                    onClear = onClear,
                    compact = true,
                    showDots = false,
                )
            }
        }
    }
}
// ─── 공통 광고 컬럼 ───
@Composable
private fun AdColumn(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // 헤더: 그라데이션 바 + (올인원 텍스트 + 바디스위치 로고)
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 세로 그라데이션 바
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(200.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.White, TealPrimary),
                        )
                    ),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "올인원 스포츠 시설 통합 운영 플랫폼",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Image(
                    painter = painterResource(R.drawable.logo_bodyswitch_kr_teal),
                    contentDescription = "바디스위치",
                    modifier = Modifier.height(48.dp),
                    contentScale = ContentScale.FillHeight,
                )
                Text(
                    text = "회원관리\n이제 더 간편하게!",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFEEB1C),
                    lineHeight = 50.sp,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize(0.9f)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ItemCard(text = "회원관리 프로그램", modifier = Modifier.weight(1f))
            ItemCard(text = "IoT 시설제어", modifier = Modifier.weight(1f))
            ItemCard(text = "무인 입출입 관리", modifier = Modifier.weight(1f))
        }
    }
}

// 광고 아이템 3개의 텍스트
@Composable
fun ItemCard(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topEnd = 999.dp, bottomEnd = 999.dp))
            .background(Color(0x334AB3BC)),
        verticalAlignment = Alignment.CenterVertically,
        // 왼쪽부터 정렬하도록 변경
        horizontalArrangement = Arrangement.Start
    ) {

        // 2. 아이콘 영역 (고정 크기)
        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_drawer_lightning),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
        }

        // 3. 아이콘과 텍스트 사이 간격 고정
        Spacer(modifier = Modifier.width(5.dp))

        // 4. 텍스트
        Text(
            text = text,
            style = TextStyle(
                fontSize = 26.sp, // 글자가 길면 조금 줄이는 것도 방법입니다.
                fontWeight = FontWeight.Black,
                color = Color.White,
                lineHeight = 40.sp
            )
        )
    }
}
// ─── QR만 표시 ───
@Composable
private fun QrOnlyContent(
    modifier: Modifier,
    cameraPermission: Boolean,
    onRequestPermission: () -> Unit,
    onQrDetected: (String) -> Unit,
) {
    Row(modifier = modifier.padding(horizontal = 16.dp)) {
        // ── 광고 영역 (공통) ──
        AdColumn(modifier = Modifier.weight(0.3f).fillMaxHeight())

        Spacer(modifier = Modifier.width(16.dp))

        // ── QR 영역 (폰 영역까지 확장) ──
        Column(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // QR 헤더 (BothCheckinContent와 동일)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_qr_check),
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("QR 체크인", fontSize = 35.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("앱의 QR코드를 스캔해 주세요", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = GrayText)
                }
            }

            // 카메라 (BothCheckinContent와 동일 스타일, 전체 너비)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xFF404040)),
                contentAlignment = Alignment.Center,
            ) {
                if (cameraPermission) {
                    CameraPreview(
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(32.dp)),
                        onQrDetected = onQrDetected,
                    )
                } else {
                    CameraPermissionPlaceholder(onRequestPermission)
                }
                QrScanOverlay(scanSize = 350.dp)
            }
        }
    }
}

// ─── 번호만 표시 (폰 영역까지 확장, BothCheckinContent와 동일 디자인) ───
@Composable
private fun PhoneOnlyContent(
    modifier: Modifier,
    phoneNumber: String,
    isLoading: Boolean,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
) {
    Row(modifier = modifier.padding(horizontal = 16.dp)) {
        // ── 광고 영역 (공통) ──
        AdColumn(modifier = Modifier.weight(0.3f).fillMaxHeight())

        Spacer(modifier = Modifier.width(16.dp))

        // ── 번호 체크인 영역 (QR 영역까지 확장) ──
        Column(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // 번호 헤더 (BothCheckinContent와 동일)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_phone_check),
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("번호 체크인", fontSize = 35.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("전화번호 뒤 8자리를 입력해 주세요", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = GrayText)
                }
            }

            // 입력바 + 키패드 (BothCheckinContent와 동일)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PhoneKeypadContent(
                    phoneNumber = phoneNumber,
                    isLoading = isLoading,
                    onDigit = onDigit,
                    onDelete = onDelete,
                    onClear = onClear,
                    compact = true,
                    showDots = false,
                )
            }
        }
    }
}

// ─── 공통 컴포넌트: 번호 키패드 ───
@Composable
private fun PhoneKeypadContent(
    phoneNumber: String,
    isLoading: Boolean,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
    compact: Boolean,
    showDots: Boolean = true,
    showInputBar: Boolean = true,
) {
    val dotSize = if (compact) 36.dp else 44.dp
    val dotSpacing = if (compact) 8.dp else 12.dp
    val dotFontSize = if (compact) 18.sp else 22.sp
    val keyHeight = if (compact) 70.dp else 83.dp
    val keyFontSize = if (compact) 30.sp else 32.sp
    val keySpacing = if (compact) 20.dp else 20.dp

    // 전화번호 포맷: 입력 없으면 빈칸, 입력 시작하면 010-XXXX-XXXX
    val formattedNumber = if (phoneNumber.isEmpty()) {
        ""
    } else {
        buildString {
            append("010-")
            append(phoneNumber.take(4))
            if (phoneNumber.length > 4) {
                append("-")
                append(phoneNumber.drop(4))
            }
        }
    }
    val fontSize = if (compact) 40.sp else 40.sp

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showInputBar) {
            // 전화번호 입력 표시 + 하단 바
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 60.dp else 80.dp)
                    .drawBehind {
                        drawLine(
                            color = Color.White.copy(alpha = 0.8f),
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 4.dp.toPx(),
                        )
                    },
                contentAlignment = Alignment.BottomStart,
            ) {
                Text(
                    text = formattedNumber,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
                )
            }

            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(color = TealPrimary, modifier = Modifier.size(32.dp))
            }

            Spacer(modifier = Modifier.height(if (compact) 12.dp else 24.dp))
        }
        Spacer(modifier = Modifier.padding(10.dp))
        // 키패드
        val keypadRows = remember { listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9")) }
        Column(verticalArrangement = Arrangement.spacedBy(keySpacing)) {
            keypadRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(keySpacing),
                ) {
                    row.forEach { key ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(keyHeight)
                                .clip(RoundedCornerShape(99.dp))
                                .background(KeyBg)
                                .clickable { onDigit(key) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(key, fontSize = keyFontSize, fontWeight = FontWeight.SemiBold, color = Color.Black)
                        }
                    }
                }
            }
            // 마지막 줄: C, 0, ←
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(keySpacing),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(keyHeight)
                        .clip(RoundedCornerShape(99.dp))
                        .background(ActionKeyBg)
                        .clickable { onClear() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("C", fontSize = (keyFontSize.value - 2).sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(keyHeight)
                        .clip(RoundedCornerShape(99.dp))
                        .background(KeyBg)
                        .clickable { onDigit("0") },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("0", fontSize = keyFontSize, fontWeight = FontWeight.SemiBold, color = Color.Black)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(keyHeight)
                        .clip(RoundedCornerShape(99.dp))
                        .background(ActionKeyBg)
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Backspace, contentDescription = "지우기", modifier = Modifier.size(24.dp), tint = Color.White)
                }
            }
        }
    }
}

// ─── 공통 컴포넌트: QR 스캔 오버레이 ───
@Composable
private fun QrScanOverlay(scanSize: Dp) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val scanSizePx = scanSize.toPx()
                val left = (size.width - scanSizePx) / 2f
                val top = (size.height - scanSizePx) / 2f
                val right = left + scanSizePx
                val bottom = top + scanSizePx
                val overlayColor = Color.Black.copy(alpha = 0.75f)

                drawRect(overlayColor, topLeft = Offset.Zero, size = androidx.compose.ui.geometry.Size(size.width, top))
                drawRect(overlayColor, topLeft = Offset(0f, bottom), size = androidx.compose.ui.geometry.Size(size.width, size.height - bottom))
                drawRect(overlayColor, topLeft = Offset(0f, top), size = androidx.compose.ui.geometry.Size(left, scanSizePx))
                drawRect(overlayColor, topLeft = Offset(right, top), size = androidx.compose.ui.geometry.Size(size.width - right, scanSizePx))
            },
    ) {
        Image(
            painter = painterResource(R.drawable.ic_qr_scan_frame),
            contentDescription = null,
            modifier = Modifier
                .size(scanSize)
                .align(Alignment.Center),
        )
    }
}

// ─── 공통 컴포넌트: 카메라 권한 없음 ───
@Composable
private fun CameraPermissionPlaceholder(onRequest: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(48.dp), tint = GrayText)
        Spacer(modifier = Modifier.height(12.dp))
        Text("카메라 권한을 허용해주세요", color = GrayText, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onRequest,
            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
            shape = RoundedCornerShape(8.dp),
        ) {
            Text("권한 허용", fontSize = 14.sp)
        }
    }
}

// ─── 상단 바 ───
@Composable
private fun TopBar(
    centerName: String,
    onStaffCall: () -> Unit,
    onDateClick: () -> Unit = {},
    switchText: String = "",
    onSwitch: () -> Unit = {},
) {
    // 시계 상태를 TopBar 안에서만 관리 → MainCheckinScreen 리컴포지션 방지
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.KOREA) }
    val timeFormat = remember { SimpleDateFormat("a hh:mm", Locale.KOREA) }
    val now = Date(currentTime)
    val calendar = Calendar.getInstance().apply { time = now }
    val dayOfWeek = arrayOf("", "일", "월", "화", "수", "목", "금", "토")[calendar.get(Calendar.DAY_OF_WEEK)]

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onDateClick() },
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text(dateFormat.format(now), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White, letterSpacing = (-0.2).sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("($dayOfWeek)", modifier = Modifier.offset(y = (-5).dp), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
            Text(timeFormat.format(now), fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = Color.White, letterSpacing = (-0.3).sp)
        }
        Text(centerName, fontSize = 28.sp, fontWeight = FontWeight.Medium, color = Color.White)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (switchText.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .border(2.dp, Color.White, RoundedCornerShape(999.dp))
                        .clickable { onSwitch() }
                        .padding(horizontal = 28.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(switchText, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Color.White)
                }
            }
            Box(
                modifier = Modifier
                    .border(2.dp, Color.White, RoundedCornerShape(999.dp))
                    .clickable { onStaffCall() }
                    .padding(horizontal = 28.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("직원호출", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Color.White)
            }
        }
    }
}

// ─── 드로어 ───
@Composable
private fun DrawerContent(
    sessionManager: SessionManager,
    roleDisplay: String,
    scope: kotlinx.coroutines.CoroutineScope,
    drawerState: androidx.compose.material3.DrawerState,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogout: () -> Unit,
) {
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
        drawerContainerColor = DarkDrawer,
    ) {
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
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White,
                )
                Text(text = roleDisplay, fontSize = 13.sp, color = TealPrimary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("지점 정보", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray,
            modifier = Modifier.padding(horizontal = 24.dp), letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(8.dp))

        DrawerInfoRow(painterResource(R.drawable.ic_drawer_store), 20.dp, "지점명", sessionManager.branchName ?: "-")

        sessionManager.address?.let { addr ->
            val full = if (sessionManager.addressDetail != null) "$addr ${sessionManager.addressDetail}" else addr
            DrawerInfoRow(painterResource(R.drawable.ic_drawer_location), 18.dp, "주소", full)
        }
        sessionManager.phone?.let { phone ->
            DrawerInfoRow(painterResource(R.drawable.ic_drawer_phone), 18.dp, "전화", phone)
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { scope.launch { drawerState.close() }; onHistoryClick() }
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(R.drawable.ic_drawer_history), contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("출입 기록", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { scope.launch { drawerState.close() }; onSettingsClick() }
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(R.drawable.ic_drawer_settings), contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("메인 설정", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.weight(1f))
        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

        TextButton(
            onClick = { scope.launch { drawerState.close() }; onLogout() },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Image(painterResource(R.drawable.ic_drawer_logout), contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("로그아웃", color = Coral, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DrawerInfoRow(painter: Painter, iconSize: Dp, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Image(painter, contentDescription = null, modifier = Modifier.size(iconSize))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, fontSize = 11.sp, color = Color.Gray)
            Text(text = value, fontSize = 14.sp, color = Color.White)
        }
    }
}

// ─── 직원호출 다이얼로그 ───
@Composable
private fun StaffCallDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .width(400.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(top = 40.dp, bottom = 24.dp, start = 32.dp, end = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("직원을 호출하시겠습니까?", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(12.dp))
            Text("직원 호출 요청 시, 담당자에게 알림이 발송됩니다", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.weight(1f).height(48.dp)
                        .border(1.dp, Color(0xFFD9D9D9), RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp)).clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) { Text("닫기", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.Gray) }
                Box(
                    modifier = Modifier.weight(1f).height(48.dp)
                        .clip(RoundedCornerShape(8.dp)).background(TealPrimary).clickable { onConfirm() },
                    contentAlignment = Alignment.Center,
                ) { Text("호출", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White) }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "메인 체크인 화면",
    widthDp = 1920,
    heightDp = 1080,
    showBackground = true,
    backgroundColor = 0xFF000000,
)
@Composable
private fun MainCheckinScreenPreview() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val autoLoginManager = com.bodyswitch.checkin.data.session.AutoLoginManager(context)
    val sessionManager = SessionManager(autoLoginManager)
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
    val checkinSettingsManager = CheckinSettingsManager(context)
    MainCheckinScreen(
        sessionManager = sessionManager,
        checkinSettingsManager = checkinSettingsManager,
        onQrScanned = {},
        onPhoneLogin = {},
        onHistoryClick = {},
        onSettingsClick = {},
        onLogout = {},
    )
}
