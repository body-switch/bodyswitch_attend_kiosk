package com.bodyswitch.checkin.ui.checkin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.bodyswitch.checkin.R
import com.bodyswitch.checkin.data.model.CoursePass
import com.bodyswitch.checkin.data.model.Reservation
import com.bodyswitch.checkin.data.model.Ticket
import com.bodyswitch.checkin.data.model.TicketType
import com.bodyswitch.checkin.ui.common.isPortrait
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale

// 피그마 디자인 컬러
private val Primary = Color(0xFF4AB3BC)
private val PrimaryBg = Color(0x294AB3BC)
private val DarkBg = Color(0xFF000000)
private val CardBg = Color(0xFF262626)
private val TextWhite = Color(0xFFFFFFFF)
private val TextGray = Color(0xFFD9D9D9)
private val TextMuted = Color(0xFFA6A6A6)
private val ProgressTrack = Color(0xFF737373)
private val Red = Color(0xFFE53935)
private val Green = Color(0xFF4CAF50)

@Composable
fun CheckinScreen(
    onBack: () -> Unit,
    onCheckinComplete: () -> Unit,
    onEmployeeAttendType: () -> Unit = {},
    onRequireLogin: () -> Unit = {},
    centerName: String = "",
    viewModel: CheckinViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showStaffCallDialog by remember { mutableStateOf(false) }

    // 비활동 감지: 5초 후 경고 팝업, 10초 후 종료
    var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showInactivityWarning by remember { mutableStateOf(false) }
    var inactivityCountdown by remember { mutableIntStateOf(5) }
    var isNavigatingBack by remember { mutableStateOf(false) }

    LaunchedEffect(lastInteraction) {
        showInactivityWarning = false
        inactivityCountdown = 5
        delay(5_000L)
        if (!isActive) return@LaunchedEffect
        showInactivityWarning = true
        while (inactivityCountdown > 0 && isActive) {
            delay(1_000L)
            inactivityCountdown--
        }
        if (isActive && !isNavigatingBack) {
            isNavigatingBack = true
            onBack()
        }
    }

    // 실시간 시계
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    LaunchedEffect(uiState.checkinDone) {
        if (uiState.checkinDone) onCheckinComplete()
    }
    LaunchedEffect(uiState.isEmployee) {
        if (uiState.isEmployee) {
            onEmployeeAttendType()
        }
    }
    LaunchedEffect(uiState.requireLogin) {
        if (uiState.requireLogin) onRequireLogin()
    }
    LaunchedEffect(uiState.autoCheckinDone) {
        if (uiState.autoCheckinDone && !isNavigatingBack) {
            isNavigatingBack = true
            onBack()
        }
    }

    val now = Date(currentTime)
    val calendar = Calendar.getInstance().apply { time = now }
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
    val timeFormat = SimpleDateFormat("a hh:mm", Locale.KOREA)
    val dayOfWeek = arrayOf("", "일", "월", "화", "수", "목", "금", "토")[calendar.get(Calendar.DAY_OF_WEEK)]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        lastInteraction = System.currentTimeMillis()
                    }
                }
            },
    ) {
        // 배경 이미지
        Image(
            painter = painterResource(R.drawable.bg_diamond),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // ── 상단 헤더 바 ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.08f)),
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
                            .clickable { showStaffCallDialog = true }
                            .padding(horizontal = 28.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("직원호출", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Color.White)
                    }
                }
            }

            // ── 로딩 / 회원 없음 / 메인 콘텐츠 분기 ──
            when {
                uiState.isLoading || uiState.isEmployee -> {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }

                uiState.member == null -> {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("회원 정보를 찾을 수 없습니다", color = TextWhite, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    if (!isNavigatingBack) {
                                        isNavigatingBack = true
                                        onBack()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            ) { Text("돌아가기") }
                        }
                    }
                }

                else -> {
                    val member = uiState.member!!
                    // 세로 모드에서는 카드 열 폭이 과하게 좁아지므로 1열로 쌓는다
                    val cardColumns = if (isPortrait()) 1 else 2
                    // PASS형 체험권(이용권형)은 "이용권" 섹션에 노출한다. 레슨형 체험권/수강권만 "수강권" 섹션.
                    val activeTickets = member.tickets.filter { it.status != "INACTIVE" && !it.isPassType }
                    val expiredTickets = member.tickets.filter { it.status == "INACTIVE" && !it.isPassType }
                    val activePassTrials = member.tickets.filter { it.status != "INACTIVE" && it.isPassType }
                    val expiredPassTrials = member.tickets.filter { it.status == "INACTIVE" && it.isPassType }
                    val activePasses = member.passes.filter { it.status != "INACTIVE" }
                    val expiredPasses = member.passes.filter { it.status == "INACTIVE" }

                    val selectedReservation = uiState.reservations.find { it.reservationId == uiState.selectedReservationId }
                    val canCheckin = when {
                        // 이용권 또는 PASS형 체험권은 예약 없이 바로 체크인 가능
                        uiState.selectedTicketIsPass -> true
                        uiState.selectedTicketType in listOf(TicketType.COURSE_TICKET, TicketType.TRIAL_TICKET) ->
                            selectedReservation != null && selectedReservation.status in listOf("ATTENDED", "RESERVED")
                        uiState.selectedTicketType == TicketType.COURSE_PASS -> true
                        else -> false
                    }

                    // ── 회원 정보 섹션 (뒤로가기 + 아바타 + 이름) ──
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                    ) {
                        // 뒤로가기 버튼 (좌측)
                        Row(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 48.dp)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                ) {
                                    if (!isNavigatingBack) {
                                        isNavigatingBack = true
                                        onBack()
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "뒤로",
                                tint = TextGray,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "뒤로가기",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextGray,
                            )
                        }

                        // 회원 정보 (중앙)
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            // 아바타
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2A2A2A)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.ic_drawer_person),
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                )
                            }
                            // 이름 + 환영 텍스트
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = member.name,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextWhite,
                                )
                                Text(
                                    text = "회원님 환영합니다!",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextMuted,
                                )
                            }
                        }
                    }

                    // ── 스크롤 가능 카드 영역 ──
                    val scrollState = rememberScrollState()
                    val coroutineScope = rememberCoroutineScope()
                    var reservationOffset by remember { mutableIntStateOf(0) }

                    // 수업 정보 나타나면 해당 위치로 스크롤
                    LaunchedEffect(uiState.reservationsLoaded) {
                        if (uiState.reservationsLoaded && reservationOffset > 0) {
                            coroutineScope.launch {
                                scrollState.animateScrollTo(reservationOffset)
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState)
                            .padding(vertical = 8.dp),
                    ) {
                        // 사용 중인 수강권
                        if (activeTickets.isNotEmpty()) {
                            SectionHeader(title = "사용 중인 수강권", count = activeTickets.size, countColor = Primary)
                            Spacer(modifier = Modifier.height(12.dp))
                            activeTickets.chunked(cardColumns).forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    row.forEach { ticket ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            TicketCard(
                                                ticket = ticket,
                                                isSelected = uiState.selectedTicketId == ticket.id,
                                                isExpired = false,
                                                onClick = { viewModel.selectTicket(ticket.id, ticket.type) },
                                            )
                                        }
                                    }
                                    if (row.size < cardColumns) Spacer(modifier = Modifier.weight(1f))
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            Column(
                                modifier = Modifier.onGloballyPositioned { coords ->
                                    reservationOffset = coords.positionInParent().y.toInt()
                                },
                            ) {
                                AnimatedVisibility(
                                    visible = uiState.selectedTicketId != null &&
                                        !uiState.selectedTicketIsPass &&
                                        uiState.selectedTicketType in listOf(TicketType.COURSE_TICKET, TicketType.TRIAL_TICKET),
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut(),
                                ) {
                                    ReservationSection(
                                        isLoading = uiState.reservationsLoading,
                                        reservations = uiState.reservations,
                                        noReservations = uiState.noReservations,
                                        selectedReservationId = uiState.selectedReservationId,
                                        onSelectReservation = { viewModel.selectReservation(it) },
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // 사용 중인 이용권 (이용권 + PASS형 체험권)
                        if (activePasses.isNotEmpty() || activePassTrials.isNotEmpty()) {
                            SectionHeader(
                                title = "사용 중인 이용권",
                                count = activePasses.size + activePassTrials.size,
                                countColor = Primary,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            // PASS형 체험권 (이용권처럼 예약 없이 입장)
                            activePassTrials.chunked(cardColumns).forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    row.forEach { ticket ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            TicketCard(
                                                ticket = ticket,
                                                isSelected = uiState.selectedTicketId == ticket.id &&
                                                    uiState.selectedTicketType == TicketType.TRIAL_TICKET,
                                                isExpired = false,
                                                onClick = { viewModel.selectTicket(ticket.id, ticket.type) },
                                            )
                                        }
                                    }
                                    if (row.size < cardColumns) Spacer(modifier = Modifier.weight(1f))
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            activePasses.chunked(cardColumns).forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    row.forEach { pass ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            PassCard(
                                                pass = pass,
                                                isSelected = uiState.selectedTicketId == pass.id &&
                                                    uiState.selectedTicketType == TicketType.COURSE_PASS,
                                                isExpired = false,
                                                onClick = { viewModel.selectTicket(pass.id, TicketType.COURSE_PASS) },
                                            )
                                        }
                                    }
                                    if (row.size < cardColumns) Spacer(modifier = Modifier.weight(1f))
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }

                        // 만료된 수강권
                        if (expiredTickets.isNotEmpty()) {
                            SectionHeader(title = "만료된 수강권", count = expiredTickets.size, countColor = TextMuted)
                            Spacer(modifier = Modifier.height(12.dp))
                            expiredTickets.chunked(cardColumns).forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    row.forEach { ticket ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            TicketCard(ticket = ticket, isSelected = false, isExpired = true, onClick = {})
                                        }
                                    }
                                    if (row.size < cardColumns) Spacer(modifier = Modifier.weight(1f))
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }

                        // 만료된 이용권 (이용권 + PASS형 체험권)
                        if (expiredPasses.isNotEmpty() || expiredPassTrials.isNotEmpty()) {
                            SectionHeader(
                                title = "만료된 이용권",
                                count = expiredPasses.size + expiredPassTrials.size,
                                countColor = TextMuted,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            expiredPassTrials.chunked(cardColumns).forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    row.forEach { ticket ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            TicketCard(ticket = ticket, isSelected = false, isExpired = true, onClick = {})
                                        }
                                    }
                                    if (row.size < cardColumns) Spacer(modifier = Modifier.weight(1f))
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            expiredPasses.chunked(cardColumns).forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    row.forEach { pass ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            PassCard(pass = pass, isSelected = false, isExpired = true, onClick = {})
                                        }
                                    }
                                    if (row.size < cardColumns) Spacer(modifier = Modifier.weight(1f))
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }

                    // ── 하단 고정 체크인 버튼 ──
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBg)
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        // 차감형 수강권/체험권만 차감 안내 (PASS형 체험권 제외)
                        val isTicket = !uiState.selectedTicketIsPass &&
                            uiState.selectedTicketType in listOf(
                                TicketType.COURSE_TICKET, TicketType.TRIAL_TICKET
                            )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (canCheckin) Color(0xFF3E9FA7) else CardBg)
                                .then(if (canCheckin) Modifier.clickable { viewModel.checkin() } else Modifier),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (canCheckin) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(48.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    // 체크 아이콘 + 체크인 텍스트
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Image(
                                            painter = painterResource(R.drawable.ic_checkin_check),
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                        )
                                        Text(
                                            text = "체크인",
                                            fontSize = 40.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                        )
                                    }
                                    // 수강권일 때만 차감 안내 텍스트
                                    if (isTicket) {
                                        Text(
                                            text = "잔여횟수 -1회 차감",
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "이용권을 선택해주세요",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted,
                                )
                            }
                        }
                    }
                }
            }
        }

        // 비활동 경고 팝업
        AnimatedVisibility(
            visible = showInactivityWarning,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp),
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 48.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1A1A1A).copy(alpha = 0.95f))
                    .border(2.dp, Primary, RoundedCornerShape(20.dp))
                    .padding(horizontal = 40.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Text(
                        text = "상호작용이 없으면 종료됩니다",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "$inactivityCountdown",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }
            }
        }

        // 스낵바
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        ) { data ->
            Snackbar(snackbarData = data, containerColor = Red, contentColor = Color.White)
        }
    }

    if (showStaffCallDialog) {
        Dialog(
            onDismissRequest = { showStaffCallDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .padding(top = 40.dp, bottom = 24.dp, start = 32.dp, end = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "직원을 호출하시겠습니까?",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "직원 호출 요청 시, 담당자에게 알림이 발송됩니다",
                    fontSize = 14.sp,
                    color = Color.Gray,
                )
                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider(color = Color(0xFFE0E0E0))
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f).height(48.dp)
                            .border(1.dp, Color(0xFFD9D9D9), RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showStaffCallDialog = false },
                        contentAlignment = Alignment.Center,
                    ) { Text("닫기", fontSize = 16.sp, color = Color.Gray) }
                    Box(
                        modifier = Modifier
                            .weight(1f).height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Primary)
                            .clickable { showStaffCallDialog = false },
                        contentAlignment = Alignment.Center,
                    ) { Text("호출", fontSize = 16.sp, color = Color.White) }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int, countColor: Color) {
    Row(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, color = TextGray, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "$count", color = countColor, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
    }
}

@Composable
private fun TicketCard(
    ticket: Ticket,
    isSelected: Boolean,
    isExpired: Boolean,
    onClick: () -> Unit,
) {
    val textColor = if (isExpired) TextMuted else TextWhite
    val accentColor = if (isExpired) TextMuted else Primary
    val badgeLabel = if (ticket.type == TicketType.TRIAL_TICKET) "체험권" else "수강권"
    val daysRemaining = calculateDaysRemaining(ticket.expireDate)
    val progress = if (ticket.usageCount > 0) ticket.remainCount.toFloat() / ticket.usageCount.toFloat() else 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) {
                    Modifier
                        .border(4.dp, Primary, RoundedCornerShape(16.dp))
                        .background(PrimaryBg, RoundedCornerShape(16.dp))
                } else {
                    Modifier.background(CardBg, RoundedCornerShape(16.dp))
                }
            )
            .clip(RoundedCornerShape(16.dp))
            .then(if (!isExpired) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = ticket.name,
                    color = textColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .border(2.dp, accentColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(text = badgeLabel, color = accentColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("유효기간", color = if (isExpired) TextMuted else TextGray, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatDateRange(ticket.startDate, ticket.expireDate),
                        color = if (isExpired) TextMuted else TextGray,
                        fontSize = 14.sp,
                    )
                }
                if (!isExpired && daysRemaining != null) {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = Primary, fontWeight = FontWeight.SemiBold)) { append("${daysRemaining}일 ") }
                            withStyle(SpanStyle(color = TextWhite)) { append("남음") }
                        },
                        fontSize = 14.sp,
                    )
                }
            }

            // PASS형 체험권은 기간제 이용권이라 잔여횟수/진행바를 표시하지 않는다
            if (!ticket.isPassType) {
                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("잔여횟수", color = if (isExpired) TextMuted else TextGray, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append("${ticket.remainCount}회") }
                                append(" / ${ticket.usageCount}회")
                            },
                            color = if (isExpired) TextMuted else TextGray,
                            fontSize = 14.sp,
                        )
                    }
                    if (!isExpired) {
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(color = Primary, fontWeight = FontWeight.SemiBold)) { append("${ticket.remainCount}회 ") }
                                withStyle(SpanStyle(color = TextWhite)) { append("남음") }
                            },
                            fontSize = 14.sp,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isExpired) ProgressTrack else PrimaryBg),
                ) {
                    if (!isExpired && progress > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .height(10.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Primary),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PassCard(
    pass: CoursePass,
    isSelected: Boolean,
    isExpired: Boolean,
    onClick: () -> Unit,
) {
    val textColor = if (isExpired) TextMuted else TextWhite
    val accentColor = if (isExpired) TextMuted else Primary
    val daysRemaining = calculateDaysRemaining(pass.expireDate)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) {
                    Modifier
                        .border(4.dp, Primary, RoundedCornerShape(16.dp))
                        .background(PrimaryBg, RoundedCornerShape(16.dp))
                } else {
                    Modifier.background(CardBg, RoundedCornerShape(16.dp))
                }
            )
            .clip(RoundedCornerShape(16.dp))
            .then(if (!isExpired) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = pass.name,
                    color = textColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .border(2.dp, accentColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(text = "이용권", color = accentColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("유효기간", color = if (isExpired) TextMuted else TextGray, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatDateRange(pass.startDate, pass.expireDate),
                        color = if (isExpired) TextMuted else TextGray,
                        fontSize = 14.sp,
                    )
                }
                if (!isExpired && daysRemaining != null) {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = Primary, fontWeight = FontWeight.SemiBold)) { append("${daysRemaining}일 ") }
                            withStyle(SpanStyle(color = TextWhite)) { append("남음") }
                        },
                        fontSize = 14.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val periodProgress = calculatePeriodProgress(pass.startDate, pass.expireDate)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isExpired) ProgressTrack else PrimaryBg),
            ) {
                if (!isExpired && periodProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(periodProgress.coerceIn(0f, 1f))
                            .height(10.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Primary),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReservationSection(
    isLoading: Boolean,
    reservations: List<Reservation>,
    noReservations: Boolean,
    selectedReservationId: Long?,
    onSelectReservation: (Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 12.dp),
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBg, RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Primary, modifier = Modifier.size(24.dp))
            }
        } else if (noReservations) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBg, RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "오늘 예약된 수업이 없습니다", color = TextMuted, fontSize = 15.sp)
            }
        } else {
            Text(text = "오늘 수업", color = TextGray, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            reservations.forEach { reservation ->
                val isCheckable = reservation.status in listOf("ATTENDED", "RESERVED")
                val isSelected = selectedReservationId == reservation.reservationId
                ReservationCard(
                    reservation = reservation,
                    isSelected = isSelected,
                    isCheckable = isCheckable,
                    onClick = { if (isCheckable) onSelectReservation(reservation.reservationId) },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ReservationCard(
    reservation: Reservation,
    isSelected: Boolean,
    isCheckable: Boolean,
    onClick: () -> Unit,
) {
    val contentAlpha = if (isCheckable) 1f else 0.4f
    val accentColor = if (isCheckable) Primary else TextMuted

    val statusLabel = when (reservation.status) {
        "ATTENDED" -> "출석완료"
        "ABSENT" -> "결석"
        "CANCELED" -> "취소"
        "WAITING" -> "대기"
        else -> null
    }
    val statusColor = when (reservation.status) {
        "ATTENDED" -> Green
        "ABSENT" -> Color(0xFFE57373)
        "CANCELED" -> Color(0xFFBDBDBD)
        "WAITING" -> Color(0xFFFFB74D)
        else -> TextMuted
    }

    val startTimeDisplay = reservation.startTime.take(5) // HH:mm

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) {
                    Modifier
                        .border(4.dp, Primary, RoundedCornerShape(16.dp))
                        .background(PrimaryBg, RoundedCornerShape(16.dp))
                } else {
                    Modifier.background(
                        if (isCheckable) CardBg else Color(0xFF1A1A1A),
                        RoundedCornerShape(16.dp),
                    )
                }
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = isCheckable) { onClick() }
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            // 왼쪽: 수업명, 시간, 강사
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reservation.courseClassName,
                    color = TextWhite.copy(alpha = contentAlpha),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "수업시간",
                        color = if (isCheckable) TextGray else TextMuted,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${startTimeDisplay} ~ ${reservation.endTime.take(5)}",
                        color = if (isCheckable) TextGray else TextMuted,
                        fontSize = 14.sp,
                    )
                }
                reservation.employeeName?.let { instructor ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "강사",
                            color = if (isCheckable) TextGray else TextMuted,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = instructor,
                            color = if (isCheckable) TextGray else TextMuted,
                            fontSize = 14.sp,
                        )
                    }
                }
            }

            // 오른쪽: classType 배지 위 / status 배지 아래 (중앙 정렬)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                reservation.classType?.let { classType ->
                    Box(
                        modifier = Modifier
                            .border(2.dp, accentColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text(text = classType, color = accentColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                }
                statusLabel?.let { label ->
                    Box(
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text(label, color = statusColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private fun calculateDaysRemaining(expireDate: String?): Long? {
    if (expireDate == null) return null
    return try {
        val expire = LocalDate.parse(expireDate, DateTimeFormatter.ISO_LOCAL_DATE)
        val today = LocalDate.now()
        val days = ChronoUnit.DAYS.between(today, expire)
        if (days >= 0) days else null
    } catch (_: Exception) {
        null
    }
}

private fun calculatePeriodProgress(startDate: String?, expireDate: String?): Float {
    if (startDate == null || expireDate == null) return 0f
    return try {
        val start = LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE)
        val end = LocalDate.parse(expireDate, DateTimeFormatter.ISO_LOCAL_DATE)
        val today = LocalDate.now()
        val total = ChronoUnit.DAYS.between(start, end).toFloat()
        val remaining = ChronoUnit.DAYS.between(today, end).toFloat().coerceAtLeast(0f)
        if (total <= 0f) 0f else (remaining / total)
    } catch (_: Exception) {
        0f
    }
}

private fun formatDateRange(startDate: String?, expireDate: String?): String {
    if (startDate == null && expireDate == null) return "-"
    val start = startDate?.replace("-", ".") ?: "?"
    val end = expireDate?.replace("-", ".") ?: "?"
    return "$start ~ $end"
}
