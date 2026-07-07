package com.bodyswitch.checkin.ui.access

import android.Manifest
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.animation.core.Animatable
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
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bodyswitch.checkin.R
import com.bodyswitch.checkin.data.session.CheckinSettingsManager
import com.bodyswitch.checkin.data.session.SessionManager
import com.bodyswitch.checkin.ui.home.StaffCallState
import com.bodyswitch.checkin.ui.home.StaffCallViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

// ── 디자인 토큰 (design_handoff_access_registration/Kiosk.dc.html) ──
private val Teal = Color(0xFF45B6B0)
private val OnTeal = Color(0xFF062B2A)
private val Yellow = Color(0xFFF4CE00)
private val TextPrimary = Color(0xFFF4F6F5)
private val TextMuted = Color(0xFF8A9299)
private val TextSoft = Color(0xFFC7D0D5)
private val TextDisabled = Color(0xFF4A545B)
private val CellBg = Color(0xFF12161A)
private val CellBorder = Color(0xFF2A333A)
private val DashColor = Color(0xFF3A444B)
private val KeyLight = Color(0xFFE7EAE9)
private val KeyText = Color(0xFF15181C)
private val DisabledBtnBg = Color(0xFF1A1F23)
private val GhostBorder = Color(0xFF3A444B)
private val StepInactiveDot = Color(0xFF20262B)
private val StepInactiveText = Color(0xFF78828A)
private val CardGradientStart = Color(0xFF171B1F)
private val CardGradientEnd = Color(0xFF0F1316)
private val AvatarBg = Color(0xFF1C2226)
private val AvatarIcon = Color(0xFF8B949B)
private val QrModule = Color(0xFF0D3B39)
private val Red = Color(0xFFE53935)
private val PanelBg = Color(0x14FFFFFF)

private const val MASKED_PHONE = "010-XXXX-XXXX"

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AccessRegistrationScreen(
    sessionManager: SessionManager,
    checkinSettingsManager: CheckinSettingsManager,
    onExit: () -> Unit,
    viewModel: AccessRegistrationViewModel = hiltViewModel(),
    staffCallViewModel: StaffCallViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showStaffCallDialog by remember { mutableStateOf(false) }
    var snackbarIsSuccess by remember { mutableStateOf(false) }

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

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarIsSuccess = false
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // 시스템 뒤로가기 = 화면 내 뒤로가기 매핑 (phone/done에서는 home으로 이탈)
    BackHandler {
        if (!viewModel.goBack()) onExit()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // 배경 다이아몬드 워터마크 (기존 화면과 동일)
        Image(
            painter = painterResource(R.drawable.bg_diamond),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        // 콘텐츠 영역
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 130.dp),
        ) {
            when (uiState.step) {
                AccessStep.PHONE -> PhoneStep(
                    uiState = uiState,
                    onDigit = viewModel::onDigit,
                    onDelete = viewModel::onDelete,
                    onClear = viewModel::onClear,
                    onNext = viewModel::submitPhone,
                    onBack = onExit,
                )
                AccessStep.CONFIRM -> ConfirmStep(
                    memberName = uiState.memberName,
                    onNo = { viewModel.goBack() },
                    onYes = viewModel::confirmMember,
                    onBack = { viewModel.goBack() },
                )
                AccessStep.METHOD -> MethodStep(
                    memberName = uiState.memberName,
                    isLoading = uiState.isLoading,
                    onPickFace = viewModel::selectFace,
                    onPickQr = viewModel::selectQr,
                    onBack = { viewModel.goBack() },
                )
                AccessStep.FACE -> FaceStep(
                    isRegistering = uiState.isRegistering,
                    onStartCapture = viewModel::startCapture,
                    onCaptured = viewModel::registerFace,
                    onCaptureError = viewModel::onCaptureError,
                    onBack = { viewModel.goBack() },
                )
                AccessStep.NO_PRODUCT -> NoProductStep(
                    memberName = uiState.memberName,
                    onPrev = { viewModel.goBack() },
                    onHome = onExit,
                    onBack = { viewModel.goBack() },
                )
                AccessStep.DONE -> DoneStep(
                    memberName = uiState.memberName,
                    issueType = uiState.issueType,
                    qrPayload = uiState.qrPayload,
                    accessGranted = uiState.accessGranted,
                    onHome = onExit,
                )
            }
        }

        // 상단 바
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PanelBg),
        ) {
            AccessTopBar(
                centerName = sessionManager.businessName ?: sessionManager.branchName ?: "",
                onRestart = viewModel::restart,
                onStaffCall = { showStaffCallDialog = true },
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
                containerColor = if (snackbarIsSuccess) Teal else Red,
                contentColor = Color.White,
            )
        }
    }

    if (showStaffCallDialog) {
        AccessStaffCallDialog(
            onDismiss = { showStaffCallDialog = false },
            onConfirm = {
                showStaffCallDialog = false
                staffCallViewModel.callStaff(checkinSettingsManager.staffPhoneNumber)
            },
        )
    }
}

// ─── 직원호출 다이얼로그 (MainCheckinScreen과 동일 스타일) ───
@Composable
private fun AccessStaffCallDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.weight(1f).height(48.dp)
                        .border(1.dp, Color(0xFFD9D9D9), RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp)).clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) { Text("닫기", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.Gray) }
                Box(
                    modifier = Modifier.weight(1f).height(48.dp)
                        .clip(RoundedCornerShape(8.dp)).background(Teal).clickable { onConfirm() },
                    contentAlignment = Alignment.Center,
                ) { Text("호출", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White) }
            }
        }
    }
}

// ─── 상단 바: 날짜/시간 · 센터명 · 출입등록(재시작) + 직원호출 ───
@Composable
private fun AccessTopBar(
    centerName: String,
    onRestart: () -> Unit,
    onStaffCall: () -> Unit,
) {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (isActive) {
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
        Column {
            Row {
                Text(dateFormat.format(now), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White, letterSpacing = (-0.2).sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("($dayOfWeek)", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
            Text(timeFormat.format(now), fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = Color.White, letterSpacing = (-0.3).sp)
        }
        Text(centerName, fontSize = 28.sp, fontWeight = FontWeight.Medium, color = Color.White)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 출입등록 (teal 채움 pill) — 클릭 시 플로우 처음(phone)부터 재시작
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Teal)
                    .clickable { onRestart() }
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Face, contentDescription = null, modifier = Modifier.size(24.dp), tint = OnTeal)
                Spacer(modifier = Modifier.width(8.dp))
                Text("출입등록", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = OnTeal)
            }
            Box(
                modifier = Modifier
                    .border(2.dp, Color.White, RoundedCornerShape(999.dp))
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { onStaffCall() }
                    .padding(horizontal = 28.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("직원호출", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Color.White)
            }
        }
    }
}

// ─── 플로우 크롬: 뒤로가기 + 단계 인디케이터 ───
@Composable
private fun FlowChrome(
    stepIndex: Int?,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "← 뒤로가기",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextSoft,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 44.dp, top = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onBack() }
                .padding(8.dp),
        )
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (stepIndex != null) {
                Spacer(modifier = Modifier.height(8.dp))
                StepIndicator(current = stepIndex)
            }
            content()
        }
    }
}

// ─── 단계 인디케이터: 전화번호 · 회원확인 · 수단선택 · 발급 ───
@Composable
private fun StepIndicator(current: Int) {
    val labels = listOf("전화번호", "회원확인", "수단선택", "발급")
    Row(verticalAlignment = Alignment.CenterVertically) {
        labels.forEachIndexed { index, label ->
            val active = index <= current
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (active) Teal else StepInactiveDot),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${index + 1}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (active) OnTeal else StepInactiveText,
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = label,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (active) TextPrimary else StepInactiveText,
                )
            }
            if (index < labels.lastIndex) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 14.dp)
                        .width(56.dp)
                        .height(3.dp)
                        .background(if (index < current) Teal else StepInactiveDot),
                )
            }
        }
    }
}

// ─── 공통 teal / 고스트 버튼 ───
@Composable
private fun TealButton(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 34.sp,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(22.dp))
            .background(if (enabled) Teal else DisabledBtnBg)
            .clickable(enabled = enabled) { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            icon()
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            color = if (enabled) OnTeal else TextDisabled,
        )
    }
}

@Composable
private fun GhostButton(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 34.sp,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(22.dp))
            .border(2.dp, GhostBorder, RoundedCornerShape(22.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, fontSize = fontSize, fontWeight = FontWeight.ExtraBold, color = TextSoft)
    }
}

// ─── 2. PHONE: 전화번호 뒤 8자리 입력 ───
@Composable
private fun PhoneStep(
    uiState: AccessRegistrationUiState,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    FlowChrome(stepIndex = 0, onBack = onBack) {
        Spacer(modifier = Modifier.height(28.dp))
        Text("휴대폰 번호를 입력해 주세요", fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("전화번호 뒤 8자리를 입력하시면 됩니다", fontSize = 24.sp, fontWeight = FontWeight.Medium, color = TextMuted)

        Spacer(modifier = Modifier.height(30.dp))

        // 8자리 셀: 4 + 대시 + 4
        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(4) { i -> DigitCell(char = uiState.digits.getOrNull(i)) }
            Box(
                modifier = Modifier
                    .padding(horizontal = 14.dp)
                    .width(24.dp)
                    .height(4.dp)
                    .background(DashColor),
            )
            repeat(4) { i -> DigitCell(char = uiState.digits.getOrNull(i + 4)) }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 키패드
        val keypadRows = remember { listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("C", "0", "⌫")) }
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            keypadRows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    row.forEach { key ->
                        val isAction = key == "C" || key == "⌫"
                        Box(
                            modifier = Modifier
                                .width(118.dp)
                                .height(74.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isAction) Teal else KeyLight)
                                .clickable {
                                    when (key) {
                                        "C" -> onClear()
                                        "⌫" -> onDelete()
                                        else -> onDigit(key)
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (key == "⌫") {
                                Icon(Icons.Default.Backspace, contentDescription = "지우기", modifier = Modifier.size(30.dp), tint = OnTeal)
                            } else {
                                Text(
                                    text = key,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isAction) OnTeal else KeyText,
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // 다음 버튼: 8자리 완료 시 활성
        if (uiState.isLoading) {
            CircularProgressIndicator(color = Teal, modifier = Modifier.size(48.dp))
        } else {
            TealButton(
                text = "다음 →",
                width = 420.dp,
                height = 88.dp,
                fontSize = 34.sp,
                enabled = uiState.digits.length == AccessRegistrationViewModel.PHONE_DIGITS,
                onClick = onNext,
            )
        }
    }
}

@Composable
private fun DigitCell(char: Char?) {
    Box(
        modifier = Modifier
            .padding(horizontal = 5.dp)
            .width(74.dp)
            .height(88.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(CellBg)
            .border(3.dp, if (char != null) Teal else CellBorder, RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = char?.toString() ?: "", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = Teal)
    }
}

// ─── 3. CONFIRM: 본인확인 (상품 보유 시에만) ───
@Composable
private fun ConfirmStep(
    memberName: String,
    onNo: () -> Unit,
    onYes: () -> Unit,
    onBack: () -> Unit,
) {
    FlowChrome(stepIndex = 1, onBack = onBack) {
        Spacer(modifier = Modifier.height(24.dp))
        // 아바타
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(AvatarBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(84.dp), tint = AvatarIcon)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(memberName, fontSize = 58.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            Spacer(modifier = Modifier.width(12.dp))
            Text("회원님", fontSize = 32.sp, fontWeight = FontWeight.SemiBold, color = TextMuted, modifier = Modifier.padding(bottom = 8.dp))
        }
        Text(MASKED_PHONE, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Teal, letterSpacing = 0.5.sp)

        Spacer(modifier = Modifier.height(16.dp))
        // 상태 칩: 이용 중인 상품이 있습니다
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Teal.copy(alpha = 0.14f))
                .border(1.5.dp, Teal.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Teal),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text("이용 중인 상품이 있습니다", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Teal)
        }

        Spacer(modifier = Modifier.height(22.dp))
        Text("본인이 맞으신가요?", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        Text("맞으시면 출입 수단 선택으로 이동합니다", fontSize = 24.sp, fontWeight = FontWeight.Medium, color = TextMuted)

        Spacer(modifier = Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
            GhostButton(text = "아니요", width = 320.dp, height = 106.dp, onClick = onNo)
            TealButton(text = "네, 맞습니다", width = 400.dp, height = 106.dp, onClick = onYes)
        }
    }
}

// ─── 4. METHOD: 출입 수단 선택 ───
@Composable
private fun MethodStep(
    memberName: String,
    isLoading: Boolean,
    onPickFace: () -> Unit,
    onPickQr: () -> Unit,
    onBack: () -> Unit,
) {
    FlowChrome(stepIndex = 2, onBack = onBack) {
        Spacer(modifier = Modifier.height(36.dp))
        Text("출입 수단을 선택해 주세요", fontSize = 46.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        Spacer(modifier = Modifier.height(10.dp))
        Text("$memberName 회원님, 등록하실 출입 방식을 선택하세요", fontSize = 26.sp, fontWeight = FontWeight.Medium, color = TextMuted)

        Spacer(modifier = Modifier.height(44.dp))
        if (isLoading) {
            CircularProgressIndicator(color = Teal, modifier = Modifier.size(64.dp))
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
                MethodCard(
                    title = "안면등록",
                    subtitle = "안면인식으로 간편하게!",
                    icon = { Icon(Icons.Default.Face, contentDescription = null, modifier = Modifier.size(76.dp), tint = Teal) },
                    onClick = onPickFace,
                )
                MethodCard(
                    title = "QR 코드",
                    subtitle = "찍고 바로 입장!",
                    icon = { Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(76.dp), tint = Teal) },
                    onClick = onPickQr,
                )
            }
        }
    }
}

@Composable
private fun MethodCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(470.dp)
            .height(420.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Brush.linearGradient(listOf(CardGradientStart, CardGradientEnd)))
            .border(2.dp, CellBorder, RoundedCornerShape(32.dp))
            .clickable { onClick() }
            .padding(40.dp),
    ) {
        Column(modifier = Modifier.align(Alignment.TopStart)) {
            Text(title, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = Teal)
            Spacer(modifier = Modifier.height(10.dp))
            Text(subtitle, fontSize = 26.sp, fontWeight = FontWeight.Medium, color = TextMuted)
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(140.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Teal.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
    }
}

// ─── 5. FACE: 안면 촬영 ───
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun FaceStep(
    isRegistering: Boolean,
    onStartCapture: () -> Unit,
    onCaptured: (String) -> Unit,
    onCaptureError: () -> Unit,
    onBack: () -> Unit,
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }
    val captureExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose { captureExecutor.shutdown() }
    }

    fun capture() {
        onStartCapture()
        imageCapture.takePicture(
            captureExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        val encoded = FaceImageEncoder.encodeToBase64Jpeg(image)
                        onCaptured(encoded)
                    } catch (e: Exception) {
                        Log.e("ACCESS_REG", "얼굴 이미지 인코딩 실패", e)
                        onCaptureError()
                    } finally {
                        image.close()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("ACCESS_REG", "촬영 실패", exception)
                    onCaptureError()
                }
            },
        )
    }

    FlowChrome(stepIndex = 3, onBack = onBack) {
        Spacer(modifier = Modifier.height(12.dp))
        Text("얼굴을 등록해 주세요", fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        Text("사각형 안에 얼굴을 맞추고 촬영 버튼을 눌러 주세요", fontSize = 24.sp, fontWeight = FontWeight.Medium, color = TextMuted)
        Spacer(modifier = Modifier.height(4.dp))
        // 재등록 = 덮어쓰기 정책 안내
        Text("촬영한 사진으로 출입 얼굴이 등록/교체됩니다", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Teal)

        Spacer(modifier = Modifier.height(18.dp))

        // 카메라 + 점선 가이드 + 촬영 오버레이
        Box(
            modifier = Modifier
                .size(480.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF404040)),
            contentAlignment = Alignment.Center,
        ) {
            if (cameraPermission.status.isGranted) {
                FaceCameraPreview(
                    imageCapture = imageCapture,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(48.dp), tint = TextMuted)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("카메라 권한을 허용해주세요", color = TextMuted, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { cameraPermission.launchPermissionRequest() },
                        colors = ButtonDefaults.buttonColors(containerColor = Teal),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("권한 허용", fontSize = 16.sp, color = OnTeal)
                    }
                }
            }
            FaceGuideOverlay()
            if (isRegistering) {
                CaptureOverlay()
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        TealButton(
            text = "촬영하기",
            width = 480.dp,
            height = 96.dp,
            fontSize = 34.sp,
            enabled = cameraPermission.status.isGranted && !isRegistering,
            icon = { Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(36.dp), tint = OnTeal) },
            onClick = { capture() },
        )
    }
}

// 점선 얼굴 가이드 + 4모서리 teal 브래킷
@Composable
private fun FaceGuideOverlay() {
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // 중앙 점선 사각 가이드
                val guideWidth = with(density) { 290.dp.toPx() }
                val guideHeight = with(density) { 335.dp.toPx() }
                val stroke = with(density) { 4.dp.toPx() }
                val corner = with(density) { 38.dp.toPx() }
                val dash = with(density) { 14.dp.toPx() }
                drawRoundRect(
                    color = Teal.copy(alpha = 0.85f),
                    topLeft = Offset((size.width - guideWidth) / 2f, (size.height - guideHeight) / 2f),
                    size = Size(guideWidth, guideHeight),
                    cornerRadius = CornerRadius(corner, corner),
                    style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))),
                )

                // 4모서리 브래킷
                val margin = with(density) { 18.dp.toPx() }
                val len = with(density) { 48.dp.toPx() }
                val bracketStroke = with(density) { 5.dp.toPx() }
                val edges = listOf(
                    Offset(margin, margin) to Pair(Offset(len, 0f), Offset(0f, len)),
                    Offset(size.width - margin, margin) to Pair(Offset(-len, 0f), Offset(0f, len)),
                    Offset(margin, size.height - margin) to Pair(Offset(len, 0f), Offset(0f, -len)),
                    Offset(size.width - margin, size.height - margin) to Pair(Offset(-len, 0f), Offset(0f, -len)),
                )
                edges.forEach { (origin, dirs) ->
                    drawLine(Teal, origin, origin + dirs.first, strokeWidth = bracketStroke)
                    drawLine(Teal, origin, origin + dirs.second, strokeWidth = bracketStroke)
                }
            },
    )
}

// 촬영 오버레이: 흰 플래시 + 스피너 + "얼굴 등록 중..." (실제 API 응답까지 표시)
@Composable
private fun CaptureOverlay() {
    val flashAlpha = remember { Animatable(0.9f) }
    LaunchedEffect(Unit) {
        flashAlpha.animateTo(0f, animationSpec = tween(durationMillis = 700))
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = flashAlpha.value)),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Teal, modifier = Modifier.size(56.dp), strokeWidth = 5.dp)
            Spacer(modifier = Modifier.height(18.dp))
            Text("얼굴 등록 중...", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}

// ─── 6. NO PRODUCT: 이용 가능한 상품 없음 ───
@Composable
private fun NoProductStep(
    memberName: String,
    onPrev: () -> Unit,
    onHome: () -> Unit,
    onBack: () -> Unit,
) {
    FlowChrome(stepIndex = null, onBack = onBack) {
        Spacer(modifier = Modifier.height(60.dp))
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(Color(0xFFF4B400).copy(alpha = 0.14f))
                .border(2.dp, Yellow.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.WarningAmber, contentDescription = null, modifier = Modifier.size(96.dp), tint = Yellow)
        }
        Spacer(modifier = Modifier.height(30.dp))
        Text("이용 가능한 상품이 없습니다", fontSize = 54.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = Yellow, fontWeight = FontWeight.Bold)) { append(memberName) }
                append(" 회원님은 유효한 수강권·이용권이 없어요.\n상품 구매 후 출입등록을 진행하실 수 있습니다.")
            },
            fontSize = 30.sp,
            fontWeight = FontWeight.Medium,
            color = TextSoft,
            textAlign = TextAlign.Center,
            lineHeight = 46.sp,
        )
        Spacer(modifier = Modifier.height(44.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
            GhostButton(text = "이전으로", width = 350.dp, height = 116.dp, fontSize = 36.sp, onClick = onPrev)
            TealButton(text = "처음으로", width = 440.dp, height = 116.dp, fontSize = 36.sp, onClick = onHome)
        }
    }
}

// ─── 7. DONE: 등록/발급 완료 ───
@Composable
private fun DoneStep(
    memberName: String,
    issueType: AccessIssueType,
    qrPayload: String?,
    accessGranted: Boolean,
    onHome: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        if (issueType == AccessIssueType.FACE) {
            Box(
                modifier = Modifier
                    .size(210.dp)
                    .clip(CircleShape)
                    .background(Teal),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(120.dp), tint = Color.White)
            }
            Spacer(modifier = Modifier.height(30.dp))
            Text("안면등록 완료", fontSize = 60.sp, fontWeight = FontWeight.ExtraBold, color = Teal)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                "$memberName 회원님, 이제 얼굴로 간편하게 출입하실 수 있어요",
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                color = TextSoft,
            )
        } else {
            // QR 발급: 흰 라운드 카드에 실제 발급 payload 렌더링
            val qrBitmap = remember(qrPayload) {
                qrPayload?.let {
                    QrCodeGenerator.generate(
                        content = it,
                        sizePx = 600,
                        moduleColor = 0xFF0D3B39.toInt(),
                        backgroundColor = android.graphics.Color.WHITE,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White)
                    .padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "출입 QR 코드",
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(140.dp), tint = QrModule)
                }
            }
            Spacer(modifier = Modifier.height(26.dp))
            Text("QR 발급 완료", fontSize = 60.sp, fontWeight = FontWeight.ExtraBold, color = Teal)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                "$memberName 회원님, 발급된 QR 코드로 바로 입장하실 수 있어요",
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                color = TextSoft,
            )
        }

        if (!accessGranted) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "출입권 등록은 데스크에 문의해 주세요",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Yellow,
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
        TealButton(text = "처음으로", width = 880.dp, height = 116.dp, fontSize = 38.sp, onClick = onHome)
    }
}
