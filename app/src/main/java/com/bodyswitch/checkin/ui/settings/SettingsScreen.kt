package com.bodyswitch.checkin.ui.settings

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.bodyswitch.checkin.R
import com.bodyswitch.checkin.data.session.CheckinSettingsManager
import kotlinx.coroutines.launch

private val BlackBg = Color(0xFF000000)
private val TopBarBg = Color(0x14FFFFFF)
private val TealPrimary = Color(0xFF4AB3BC)
private val TealCardBg = Color(0xFF4AB3BC).copy(alpha = 0.16f)
private val DarkCard = Color(0xFF262626)
private val GrayText = Color(0xFFD9D9D9)
private val Red = Color(0xFFE53935)

@Composable
fun SettingsScreen(
    checkinSettingsManager: CheckinSettingsManager,
    onBack: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    var qrEnabled by remember { mutableStateOf(checkinSettingsManager.qrCheckinEnabled) }
    var phoneEnabled by remember { mutableStateOf(checkinSettingsManager.phoneCheckinEnabled) }
    var staffPhone by remember { mutableStateOf(checkinSettingsManager.staffPhoneNumber) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showSaveDialog by remember { mutableStateOf(false) }

    // ── 출입문 설정 상태 ──
    val doorState by settingsViewModel.doorState.collectAsState()
    var doorEnabled by remember { mutableStateOf(checkinSettingsManager.doorOpenEnabled) }
    var selectedSensorId by remember { mutableStateOf(checkinSettingsManager.doorSensorId) }
    var selectedRoomName by remember { mutableStateOf(checkinSettingsManager.doorRoomName) }

    // 도어 목록 로드 후 보정: 저장된 센서가 목록에 없으면 초기화, 단일 도어면 자동 선택
    LaunchedEffect(doorState.connected, doorState.doors) {
        if (!doorState.connected) return@LaunchedEffect
        val doors = doorState.doors
        if (doors.isEmpty()) return@LaunchedEffect
        if (doors.none { it.sensorId == selectedSensorId }) {
            selectedSensorId = ""
            selectedRoomName = ""
        }
        if (doors.size == 1) {
            selectedSensorId = doors[0].sensorId
            selectedRoomName = doors[0].roomName ?: ""
        }
    }

    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackBg)
            .imePadding()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { focusManager.clearFocus() },
    ) {
        // ─── 상단 바 ───
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(TopBarBg)
                .padding(horizontal = 40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onBack() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = GrayText,
                    modifier = Modifier.size(26.dp),
                )
                Text(
                    text = "뒤로가기",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = GrayText,
                )
            }

            Box(
                modifier = Modifier
                    .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showSaveDialog = true }
                    .padding(horizontal = 32.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "저장",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GrayText,
                )
            }
        }

        // ─── 본문 ───
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 72.dp)
                .verticalScroll(rememberScrollState())
                .padding(top = 28.dp, start = 32.dp, end = 32.dp, bottom = 40.dp),
        ) {
            // ── 체크인 방식 선택 ──
            Text(
                text = "체크인 방식 선택",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = TealPrimary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "메인 화면에 표시할 체크인 방식을 선택하세요 (최소 1개)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = GrayText,
            )

            Spacer(modifier = Modifier.height(28.dp))

            CheckinOptionCard(
                iconRes = R.drawable.ic_qr_check,
                title = "QR 체크인",
                subtitle = "앱의 QR코드를 스캔해 인증하는 방식",
                isEnabled = qrEnabled,
                onClick = {
                    if (qrEnabled && !phoneEnabled) {
                        scope.launch { snackbarHostState.showSnackbar("최소 1개의 체크인 방식을 선택해야 합니다") }
                        return@CheckinOptionCard
                    }
                    qrEnabled = !qrEnabled
                },
            )

            Spacer(modifier = Modifier.height(20.dp))

            CheckinOptionCard(
                iconRes = R.drawable.ic_phone_check,
                title = "번호 체크인",
                subtitle = "전화번호 뒤 8자리를 입력해 인증하는 방식",
                isEnabled = phoneEnabled,
                onClick = {
                    if (phoneEnabled && !qrEnabled) {
                        scope.launch { snackbarHostState.showSnackbar("최소 1개의 체크인 방식을 선택해야 합니다") }
                        return@CheckinOptionCard
                    }
                    phoneEnabled = !phoneEnabled
                },
            )

            Spacer(modifier = Modifier.height(48.dp))

            // ── 직원 연락처 ──
            Text(
                text = "직원 연락처",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = TealPrimary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "직원 호출 시 알림톡을 받을 연락처를 입력하세요",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = GrayText,
            )

            Spacer(modifier = Modifier.height(28.dp))

            StaffPhoneCard(
                phoneNumber = staffPhone,
                onPhoneChange = { staffPhone = formatPhoneNumber(it) },
            )

            Spacer(modifier = Modifier.height(48.dp))

            // ── 출입문 설정 ──
            Text(
                text = "출입문 설정",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = TealPrimary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "체크인 성공 시 출입문(IoT)을 자동으로 열어줍니다",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = GrayText,
            )

            Spacer(modifier = Modifier.height(28.dp))

            when {
                doorState.loading -> {
                    Text(
                        text = "출입문 정보를 불러오는 중...",
                        fontSize = 18.sp,
                        color = GrayText,
                    )
                }
                doorState.loadFailed -> {
                    DoorNoticeCard(text = "출입문 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.")
                }
                !doorState.connected -> {
                    DoorNoticeCard(
                        text = "IoT 미연동 지점입니다. 출입문 자동 열림을 사용하려면 관리자에게 문의하세요.",
                    )
                }
                doorState.doors.isEmpty() -> {
                    DoorNoticeCard(
                        text = "등록된 출입문이 없습니다. 관리자에게 문의하세요.",
                    )
                }
                else -> {
                    DoorToggleCard(
                        isEnabled = doorEnabled,
                        onClick = { doorEnabled = !doorEnabled },
                    )

                    if (doorEnabled && doorState.doors.size > 1) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "열어줄 출입문 선택",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = GrayText,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        doorState.doors.forEach { door ->
                            DoorSelectCard(
                                roomName = door.roomName ?: door.sensorId,
                                isSelected = door.sensorId == selectedSensorId,
                                onClick = {
                                    selectedSensorId = door.sensorId
                                    selectedRoomName = door.roomName ?: ""
                                },
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        ) { data ->
            Snackbar(snackbarData = data, containerColor = Red, contentColor = Color.White)
        }
    }

    if (showSaveDialog) {
        SaveConfirmDialog(
            onDismiss = { showSaveDialog = false },
            onConfirm = {
                checkinSettingsManager.qrCheckinEnabled = qrEnabled
                checkinSettingsManager.phoneCheckinEnabled = phoneEnabled
                checkinSettingsManager.staffPhoneNumber = staffPhone

                // 출입문 설정: 연동 지점 + 토글 ON + 선택된 도어가 있을 때만 활성 저장
                val canUseDoor = doorState.connected && doorState.doors.isNotEmpty()
                if (canUseDoor && doorEnabled) {
                    val door = doorState.doors.firstOrNull { it.sensorId == selectedSensorId }
                        ?: doorState.doors.first()
                    checkinSettingsManager.doorOpenEnabled = true
                    checkinSettingsManager.doorSensorId = door.sensorId
                    checkinSettingsManager.doorRoomName = door.roomName ?: ""
                } else {
                    checkinSettingsManager.doorOpenEnabled = false
                }

                showSaveDialog = false
                onBack()
            },
        )
    }
}

private fun formatPhoneNumber(input: String): String {
    val digits = input.filter { it.isDigit() }.take(11)
    return when {
        digits.length <= 3 -> digits
        digits.length <= 7 -> "${digits.substring(0, 3)}-${digits.substring(3)}"
        else -> "${digits.substring(0, 3)}-${digits.substring(3, 7)}-${digits.substring(7)}"
    }
}

@Composable
private fun CheckinOptionCard(
    iconRes: Int,
    title: String,
    subtitle: String,
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isEnabled) TealCardBg else DarkCard)
            .clickable { onClick() }
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = GrayText)
        }

        Spacer(modifier = Modifier.width(24.dp))

        if (isEnabled) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(TealPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
        } else {
            Box(modifier = Modifier.size(40.dp).border(2.dp, GrayText, RoundedCornerShape(6.dp)))
        }
    }
}

@Composable
private fun DoorNoticeCard(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_door),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
            )
        }
        Spacer(modifier = Modifier.width(24.dp))
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = GrayText,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DoorToggleCard(isEnabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isEnabled) TealCardBg else DarkCard)
            .clickable { onClick() }
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_door),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "체크인 시 출입문 자동 열림",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "체크인 성공 시 선택한 출입문을 1회 열어줍니다",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = GrayText,
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        if (isEnabled) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(TealPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
        } else {
            Box(modifier = Modifier.size(40.dp).border(2.dp, GrayText, RoundedCornerShape(6.dp)))
        }
    }
}

@Composable
private fun DoorSelectCard(roomName: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) TealCardBg else DarkCard)
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isSelected) TealPrimary else Color.Transparent)
                .border(2.dp, if (isSelected) TealPrimary else GrayText, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = roomName,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StaffPhoneCard(
    phoneNumber: String,
    onPhoneChange: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    var fieldValue by remember(phoneNumber) {
        mutableStateOf(TextFieldValue(text = phoneNumber, selection = TextRange(phoneNumber.length)))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { focusRequester.requestFocus() }
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = null,
                tint = TealPrimary,
                modifier = Modifier.size(36.dp),
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "직원 전화번호",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = GrayText.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.height(6.dp))
            BasicTextField(
                value = fieldValue,
                onValueChange = { newValue ->
                    val formatted = formatPhoneNumber(newValue.text)
                    fieldValue = TextFieldValue(
                        text = formatted,
                        selection = TextRange(formatted.length),
                    )
                    onPhoneChange(formatted)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                textStyle = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                ),
                cursorBrush = SolidColor(TealPrimary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box {
                        if (fieldValue.text.isEmpty()) {
                            Text(
                                text = "010-0000-0000",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.2f),
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }

    }
}

@Composable
private fun SaveConfirmDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .width(420.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E1E2E))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                .padding(top = 40.dp, bottom = 28.dp, start = 32.dp, end = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "저장하시겠습니까?",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "변경된 설정이 적용됩니다",
                fontSize = 14.sp,
                color = GrayText,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .border(1.dp, GrayText.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "취소", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = GrayText)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(TealPrimary)
                        .clickable { onConfirm() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "확인", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White)
                }
            }
        }
    }
}
