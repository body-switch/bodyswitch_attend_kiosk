package com.bodyswitch.checkin.ui.access

import android.Manifest
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bodyswitch.checkin.R
import com.bodyswitch.checkin.data.api.dto.MemberCandidate
import com.bodyswitch.checkin.data.session.CheckinSettingsManager
import com.bodyswitch.checkin.data.session.SessionManager
import com.bodyswitch.checkin.ui.home.StaffCallState
import com.bodyswitch.checkin.ui.home.StaffCallViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
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

// 무인 키오스크 방치 방지 - 조작이 없으면 홈으로 복귀
private const val IDLE_TIMEOUT_SECONDS = 60
private const val IDLE_WARNING_SECONDS = 10

// 이 폭 미만이면 진행 인디케이터를 컴팩트하게 줄여 뒤로가기와 한 줄에 유지한다
private val STEP_INDICATOR_COMPACT_WIDTH = 820.dp

// 얼굴 가이드 사각형이 카메라 프리뷰에서 차지하는 비율 (기존 480dp 기준 290x335dp)
private const val GUIDE_WIDTH_RATIO = 0.60f
private const val GUIDE_HEIGHT_RATIO = 0.70f
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
// 안면 규격 통과(UBio 품질체크 예상 통과) 시 강조하는 초록
private val SuccessGreen = Color(0xFF22C55E)

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

    // 무인 키오스크 - 일정 시간 조작이 없으면 개인정보가 뜬 채 방치되지 않도록 홈으로 복귀.
    // 터치·단계 전환마다 타이머를 되감고, 통신/촬영 중에는 세지 않는다.
    var interactionTick by remember { mutableIntStateOf(0) }
    var idleRemaining by remember { mutableIntStateOf(IDLE_TIMEOUT_SECONDS) }
    LaunchedEffect(interactionTick, uiState.step, uiState.isLoading, uiState.isRegistering) {
        idleRemaining = IDLE_TIMEOUT_SECONDS
        if (uiState.isLoading || uiState.isRegistering) return@LaunchedEffect
        while (idleRemaining > 0) {
            delay(1000L)
            idleRemaining--
        }
        onExit()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press) {
                            interactionTick++
                        }
                    }
                }
            },
    ) {
        // 배경 다이아몬드 워터마크 (기존 화면과 동일)
        Image(
            painter = painterResource(R.drawable.bg_diamond),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        // 상단바 + 콘텐츠 영역.
        // 상단바를 Column에 실제로 배치해 자리를 차지하게 한다. 높이를 하드코딩하지 않으므로
        // 기기 해상도·방향과 무관하게 콘텐츠가 겹치지 않는다.
        Column(modifier = Modifier.fillMaxSize()) {
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

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when (uiState.step) {
                AccessStep.PHONE -> PhoneStep(
                    uiState = uiState,
                    onDigit = viewModel::onDigit,
                    onDelete = viewModel::onDelete,
                    onClear = viewModel::onClear,
                    onNext = viewModel::submitPhone,
                    onBack = onExit,
                )
                AccessStep.SELECT_MEMBER -> SelectMemberStep(
                    candidates = uiState.candidates,
                    isLoading = uiState.isLoading,
                    onSelect = viewModel::selectMember,
                    onBack = { viewModel.goBack() },
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
        }

        // 자동 복귀 임박 안내 (마지막 IDLE_WARNING_SECONDS 초)
        if (idleRemaining in 1..IDLE_WARNING_SECONDS) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Yellow)
                    .padding(horizontal = 28.dp, vertical = 14.dp),
            ) {
                Text(
                    "${idleRemaining}초 후 처음 화면으로 돌아갑니다",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = OnTeal,
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
        Text(
            centerName,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        )
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
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 뒤로가기와 진행 인디케이터는 항상 같은 줄에 둔다.
        // 좁은 화면(8인치급 ~600dp)에서는 인디케이터를 컴팩트하게 줄여 겹침을 막는다.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
        ) {
            val compact = maxWidth < STEP_INDICATOR_COMPACT_WIDTH
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                BackChip(onBack = onBack, modifier = Modifier.align(Alignment.CenterStart).padding(start = 24.dp))
                if (stepIndex != null) {
                    StepIndicator(current = stepIndex, compact = compact)
                }
            }
        }

        if (scrollable) {
            // 콘텐츠(스크롤) + 하단 "아래로 넘기기" 안내
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    content()
                    Spacer(modifier = Modifier.height(40.dp))
                }
                // 아직 아래에 더 볼 내용이 있으면 안내를 노출 (다 내리면 자동으로 사라짐)
                ScrollDownHint(
                    visible = scrollState.value < scrollState.maxValue,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                )
            }
        } else {
            // 스크롤 없이 남은 높이를 콘텐츠가 그대로 쓴다 (카메라처럼 높이에 맞춰야 하는 화면)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                content()
            }
        }
    }
}

// ─── 작은 뒤로가기 칩 ───
@Composable
private fun BackChip(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(PanelBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onBack() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("←", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextSoft)
        Spacer(modifier = Modifier.width(6.dp))
        Text("뒤로", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextSoft)
    }
}

// ─── 스크롤 안내: 어르신도 알아보게 큰 화살표 + 문구 (더 볼 내용 있을 때만) ───
@Composable
private fun ScrollDownHint(visible: Boolean, modifier: Modifier = Modifier) {
    if (!visible) return
    val transition = rememberInfiniteTransition(label = "scrollHint")
    val bounce by transition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bounce",
    )
    Column(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0xFF0B0E11).copy(alpha = 0.9f)),
                ),
            )
            .padding(top = 28.dp, bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("아래로 넘겨서 계속 보기", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextSoft)
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = Teal,
            modifier = Modifier
                .size(52.dp)
                .offset(y = bounce.dp),
        )
    }
}

// ─── 단계 인디케이터: 전화번호 · 회원확인 · 수단선택 · 발급 ───
@Composable
private fun StepIndicator(current: Int, compact: Boolean = false) {
    val labels = listOf("전화번호", "회원확인", "수단선택", "발급")
    // 뒤로가기 칩과 같은 라인에 들어가도록 컴팩트하게 구성.
    // compact = 좁은 화면. 가로 배치는 그대로 두고 크기만 줄여 뒤로가기와 같은 높이를 유지한다.
    val dotSize = if (compact) 22.dp else 34.dp
    val numberSize = if (compact) 12.sp else 17.sp
    val labelSize = if (compact) 13.sp else 18.sp
    val dotGap = if (compact) 4.dp else 8.dp
    val connectorWidth = if (compact) 12.dp else 30.dp
    val connectorGap = if (compact) 5.dp else 10.dp

    Row(verticalAlignment = Alignment.CenterVertically) {
        labels.forEachIndexed { index, label ->
            val active = index <= current
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(if (active) Teal else StepInactiveDot),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${index + 1}",
                        fontSize = numberSize,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (active) OnTeal else StepInactiveText,
                    )
                }
                Spacer(modifier = Modifier.width(dotGap))
                Text(
                    text = label,
                    fontSize = labelSize,
                    fontWeight = FontWeight.Bold,
                    color = if (active) TextPrimary else StepInactiveText,
                    maxLines = 1,
                )
            }
            if (index < labels.lastIndex) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = connectorGap)
                        .width(connectorWidth)
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
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp? = null,
    fontSize: androidx.compose.ui.unit.TextUnit = 34.sp,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier)
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
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp? = null,
    fontSize: androidx.compose.ui.unit.TextUnit = 34.sp,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier)
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

        // 8자리 셀: 4 + 대시 + 4 (폭 비례로 어떤 태블릿에서도 맞게)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(4) { i -> DigitCell(char = uiState.digits.getOrNull(i), modifier = Modifier.weight(1f)) }
            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .width(20.dp)
                    .height(4.dp)
                    .background(DashColor),
            )
            repeat(4) { i -> DigitCell(char = uiState.digits.getOrNull(i + 4), modifier = Modifier.weight(1f)) }
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
                height = 88.dp,
                fontSize = 34.sp,
                enabled = uiState.digits.length == AccessRegistrationViewModel.PHONE_DIGITS,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .padding(horizontal = 24.dp),
                onClick = onNext,
            )
        }
    }
}

@Composable
private fun DigitCell(char: Char?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(88.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(CellBg)
            .border(3.dp, if (char != null) Teal else CellBorder, RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = char?.toString() ?: "", fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = Teal)
    }
}

// ─── 3. CONFIRM: 본인확인 (상품 보유 시에만) ───
// ─── 2-1. SELECT_MEMBER: 같은 번호를 쓰는 회원이 여럿일 때 본인 선택 ───
// 동명이인이 있을 수 있어 이름만으로는 못 고른다. 생년은 가리고 월일만 함께 보여준다.
@Composable
private fun SelectMemberStep(
    candidates: List<MemberCandidate>,
    isLoading: Boolean,
    onSelect: (String) -> Unit,
    onBack: () -> Unit,
) {
    FlowChrome(stepIndex = 0, onBack = onBack) {
        Spacer(modifier = Modifier.height(24.dp))
        Text("본인을 선택해 주세요", fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "이 번호로 등록된 회원이 여러 명입니다",
            fontSize = 26.sp,
            fontWeight = FontWeight.Medium,
            color = TextMuted,
        )
        Spacer(modifier = Modifier.height(28.dp))

        if (isLoading) {
            CircularProgressIndicator(color = Teal, modifier = Modifier.size(72.dp))
            return@FlowChrome
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            candidates.forEach { candidate ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(AvatarBg)
                        .border(1.5.dp, CellBorder, RoundedCornerShape(24.dp))
                        .clickable { onSelect(candidate.memberId) }
                        .padding(horizontal = 28.dp, vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(52.dp),
                        tint = AvatarIcon,
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Column {
                        Text(
                            candidate.name,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "생년월일 ${candidate.maskedBirthDate}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            color = Teal,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "본인을 못 찾으시면 데스크에 문의해 주세요",
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = TextMuted,
        )
    }
}

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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            GhostButton(text = "아니요", height = 106.dp, modifier = Modifier.weight(1f), onClick = onNo)
            TealButton(text = "네, 맞습니다", height = 106.dp, modifier = Modifier.weight(1.4f), onClick = onYes)
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
            // 세로 화면에서는 카드를 세로로 쌓아 오버플로우 방지
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                MethodCard(
                    title = "안면등록",
                    subtitle = "안면인식으로 간편하게!",
                    icon = { Icon(Icons.Default.Face, contentDescription = null, modifier = Modifier.size(76.dp), tint = Teal) },
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onPickFace,
                )
                MethodCard(
                    title = "QR 코드",
                    subtitle = "찍고 바로 입장!",
                    icon = { Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(76.dp), tint = Teal) },
                    modifier = Modifier.fillMaxWidth(),
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
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 240.dp,
) {
    Box(
        modifier = modifier
            .height(height)
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

    // 실시간 얼굴 검증 결과 (UBio 규격 충족 여부). 초기값 = 얼굴 미검출.
    var faceResult by remember {
        mutableStateOf(FaceValidationResult(false, "얼굴을 카메라에 비춰주세요"))
    }
    // 촬영(등록) 중에는 마지막 상태를 유지 (오버레이 위 프리뷰 분석은 멈춤)
    val faceValid = faceResult.isValid && !isRegistering

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

    // 안내 문구 없이 카메라와 촬영 버튼만 둔다. 남은 높이에 정사각형 프리뷰를 맞춰 스크롤을 없앤다.
    FlowChrome(stepIndex = 3, onBack = onBack, scrollable = false) {
        Spacer(modifier = Modifier.height(12.dp))

        // 카메라 + 실시간 가이드 + 촬영 오버레이 (남은 높이에 맞춘 정사각형)
        // 규격 충족 시 박스 테두리가 초록으로 바뀌어 "지금 찍으면 됩니다"를 알림
        Box(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f, matchHeightConstraintsFirst = true)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF404040))
                .border(
                    width = if (faceValid) 6.dp else 0.dp,
                    color = if (faceValid) SuccessGreen else Color.Transparent,
                    shape = RoundedCornerShape(32.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (cameraPermission.status.isGranted) {
                FaceCameraPreview(
                    imageCapture = imageCapture,
                    onFaceResult = { faceResult = it },
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
            FaceGuideOverlay(valid = faceValid)
            // 규격 충족 시 상단에 체크 배지
            if (faceValid) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 20.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(SuccessGreen)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(26.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("좋아요!", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                }
            }
            if (isRegistering) {
                CaptureOverlay()
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 촬영 버튼: 규격을 충족(초록)했을 때만 활성 → UBio 품질체크 탈락 최소화
        TealButton(
            text = "촬영하기",
            height = 96.dp,
            fontSize = 34.sp,
            enabled = faceValid,
            icon = { Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(36.dp), tint = if (faceValid) OnTeal else TextDisabled) },
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .padding(horizontal = 24.dp),
            onClick = { capture() },
        )
        Spacer(modifier = Modifier.height(20.dp))
    }
}

// 얼굴 가이드 + 4모서리 브래킷.
// valid=false → teal 점선(맞춰주세요), valid=true → 초록 실선(규격 충족).
@Composable
private fun FaceGuideOverlay(valid: Boolean) {
    val density = LocalDensity.current
    val guideColor = if (valid) SuccessGreen else Teal.copy(alpha = 0.85f)
    val bracketColor = if (valid) SuccessGreen else Teal
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // 중앙 사각 가이드 (충족 시 실선·굵게, 미충족 시 점선).
                // 프리뷰가 남은 높이에 맞춰 줄어들 수 있으므로 고정 dp가 아닌 비율로 그린다.
                val guideWidth = size.width * GUIDE_WIDTH_RATIO
                val guideHeight = size.height * GUIDE_HEIGHT_RATIO
                val stroke = with(density) { (if (valid) 6 else 4).dp.toPx() }
                val corner = with(density) { 38.dp.toPx() }
                val dash = with(density) { 14.dp.toPx() }
                drawRoundRect(
                    color = guideColor,
                    topLeft = Offset((size.width - guideWidth) / 2f, (size.height - guideHeight) / 2f),
                    size = Size(guideWidth, guideHeight),
                    cornerRadius = CornerRadius(corner, corner),
                    style = Stroke(
                        width = stroke,
                        pathEffect = if (valid) null else PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
                    ),
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
                    drawLine(bracketColor, origin, origin + dirs.first, strokeWidth = bracketStroke)
                    drawLine(bracketColor, origin, origin + dirs.second, strokeWidth = bracketStroke)
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 820.dp)
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            GhostButton(text = "이전으로", height = 116.dp, fontSize = 36.sp, modifier = Modifier.weight(1f), onClick = onPrev)
            TealButton(text = "처음으로", height = 116.dp, fontSize = 36.sp, modifier = Modifier.weight(1.25f), onClick = onHome)
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
                    .background(if (accessGranted) Teal else Yellow),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (accessGranted) Icons.Default.Check else Icons.Default.WarningAmber,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = if (accessGranted) Color.White else OnTeal,
                )
            }
            Spacer(modifier = Modifier.height(30.dp))
            Text(
                if (accessGranted) "안면등록 완료" else "안면 정보만 등록됨",
                fontSize = 60.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (accessGranted) Teal else Yellow,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                if (accessGranted) {
                    "$memberName 회원님, 이제 얼굴로 간편하게 출입하실 수 있어요"
                } else {
                    "$memberName 회원님, 출입 등록은 아직 완료되지 않았어요"
                },
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                color = TextSoft,
            )
        } else {
            // QR 발급: 흰 라운드 카드에 실제 발급 payload 렌더링
            // 600px 비트맵 생성은 백그라운드(Default)에서 → 완료 화면 진입 시 프레임 끊김 방지
            val qrBitmap by produceState<android.graphics.Bitmap?>(initialValue = null, qrPayload) {
                value = qrPayload?.let {
                    withContext(Dispatchers.Default) {
                        QrCodeGenerator.generate(
                            content = it,
                            sizePx = 600,
                            moduleColor = 0xFF0D3B39.toInt(),
                            backgroundColor = android.graphics.Color.WHITE,
                        )
                    }
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
                val bitmap = qrBitmap
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
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
                if (accessGranted) {
                    "$memberName 회원님, 발급된 QR 코드로 바로 입장하실 수 있어요"
                } else {
                    "$memberName 회원님, QR 코드가 발급되었어요"
                },
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
        TealButton(
            text = "처음으로",
            height = 116.dp,
            fontSize = 38.sp,
            modifier = Modifier
                .padding(horizontal = 48.dp)
                .fillMaxWidth()
                .widthIn(max = 880.dp),
            onClick = onHome,
        )
    }
}
