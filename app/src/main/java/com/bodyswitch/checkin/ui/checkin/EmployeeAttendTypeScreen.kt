package com.bodyswitch.checkin.ui.checkin

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bodyswitch.checkin.R
import com.bodyswitch.checkin.data.api.KioskApi
import com.bodyswitch.checkin.data.api.dto.EmployeeCheckinRequest
import com.bodyswitch.checkin.data.api.dto.ErrorResponse
import com.bodyswitch.checkin.data.network.NetworkMonitor
import com.bodyswitch.checkin.data.session.EmployeeLoginHolder
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

// ── ViewModel ──

data class AttendTypeUiState(
    val isLoading: Boolean = false,
    val done: Boolean = false,
    val selectedAttendType: String? = null,
    val employeeName: String? = null,
    val checkinTime: String? = null,
    val entryCount: Int? = null,
    val exitCount: Int? = null,
    val message: String? = null,
    val error: String? = null,
)

@HiltViewModel
class EmployeeAttendTypeViewModel @Inject constructor(
    private val api: KioskApi,
    private val moshi: Moshi,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    val employeeName: String = EmployeeLoginHolder.employeeName ?: ""

    private val _uiState = MutableStateFlow(AttendTypeUiState())
    val uiState: StateFlow<AttendTypeUiState> = _uiState.asStateFlow()

    fun select(attendType: String, memo: String?) {
        val token = EmployeeLoginHolder.token ?: return
        val branchId = EmployeeLoginHolder.branchId ?: return
        val method = EmployeeLoginHolder.checkInMethod ?: "QR"

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = api.employeeCheckin(
                    authorization = "Bearer $token",
                    request = EmployeeCheckinRequest(
                        branchId = branchId,
                        checkInMethod = method,
                        attendType = attendType,
                        memo = memo?.ifBlank { null },
                    ),
                )
                Log.d("CHECKIN", "직원 $attendType 처리 성공: ${response.employeeName}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    done = true,
                    selectedAttendType = attendType,
                    employeeName = response.employeeName,
                    checkinTime = response.checkinTime,
                    entryCount = response.entryCount,
                    exitCount = response.exitCount,
                    message = response.message,
                )
                EmployeeLoginHolder.clear()
            } catch (e: retrofit2.HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                val errorMsg = try {
                    moshi.adapter(ErrorResponse::class.java)
                        .fromJson(errorBody ?: "")?.message
                } catch (_: Exception) {
                    null
                } ?: "처리에 실패했습니다 (${e.code()})"
                Log.e("CHECKIN", "직원 $attendType 실패: $errorMsg")
                _uiState.value = _uiState.value.copy(isLoading = false, error = errorMsg)
            } catch (e: Exception) {
                Log.e("CHECKIN", "직원 $attendType 네트워크 오류", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = networkMonitor.networkErrorMessage(),
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

// ── Screen ──

private val TealPrimary = Color(0xFF4AB3BC)
private val TealBg = Color(0x294AB3BC)
private val RedButton = Color(0xFFEF4444)
private val RedBg = Color(0x29EF4444)
private val ScreenDarkBg = Color(0xFF000000)
private val CardBg = Color(0xFF1E1E1E)
private val ScreenGrayText = Color(0xFFA6A6A6)

@Composable
fun EmployeeAttendTypeScreen(
    onComplete: (name: String, time: String?, count: Int?, exitCount: Int?, attendType: String) -> Unit,
    onBack: () -> Unit,
    centerName: String = "",
    viewModel: EmployeeAttendTypeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 메모 입력
    var memo by remember { mutableStateOf("") }

    // 비활동 감지: 10초 후 경고 팝업, 5초 후 종료
    var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showInactivityWarning by remember { mutableStateOf(false) }
    var inactivityCountdown by remember { mutableIntStateOf(5) }
    var isNavigatingBack by remember { mutableStateOf(false) }

    LaunchedEffect(lastInteraction) {
        showInactivityWarning = false
        inactivityCountdown = 5
        delay(10_000L)
        if (!isActive) return@LaunchedEffect
        showInactivityWarning = true
        while (inactivityCountdown > 0 && isActive) {
            delay(1_000L)
            inactivityCountdown--
        }
        if (isActive && !isNavigatingBack) {
            isNavigatingBack = true
            EmployeeLoginHolder.clear()
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

    LaunchedEffect(uiState.done) {
        if (uiState.done) {
            onComplete(
                uiState.employeeName ?: viewModel.employeeName,
                uiState.checkinTime,
                uiState.entryCount,
                uiState.exitCount,
                uiState.selectedAttendType ?: "ENTRY",
            )
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
            .background(ScreenDarkBg)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        lastInteraction = System.currentTimeMillis()
                    }
                }
            },
    ) {
        Image(
            painter = painterResource(R.drawable.bg_diamond),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // 상단 바
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
                    // 돌아가기 버튼
                    Box(
                        modifier = Modifier
                            .border(2.dp, Color.White, RoundedCornerShape(999.dp))
                            .clickable {
                                if (!isNavigatingBack) {
                                    isNavigatingBack = true
                                    EmployeeLoginHolder.clear()
                                    onBack()
                                }
                            }
                            .padding(horizontal = 28.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("돌아가기", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Color.White)
                    }
                }
            }

            // 중앙 콘텐츠
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = TealPrimary, modifier = Modifier.size(64.dp))
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // 배경 카드
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(CardBg)
                                .padding(horizontal = 48.dp, vertical = 40.dp),
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = "${viewModel.employeeName} 님",
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "근무 유형을 선택해주세요",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ScreenGrayText,
                                )

                                Spacer(modifier = Modifier.height(48.dp))

                                // 버튼 클릭 상태
                                var entryPressed by remember { mutableStateOf(false) }
                                var exitPressed by remember { mutableStateOf(false) }
                                val entryFill by animateFloatAsState(
                                    targetValue = if (entryPressed) 1f else 0f,
                                    animationSpec = tween(300),
                                    label = "entryFill",
                                )
                                val exitFill by animateFloatAsState(
                                    targetValue = if (exitPressed) 1f else 0f,
                                    animationSpec = tween(300),
                                    label = "exitFill",
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    // 출근 버튼
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(140.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(TealBg)
                                            .border(2.dp, TealPrimary, RoundedCornerShape(20.dp))
                                            .clickable(
                                                indication = null,
                                                interactionSource = remember { MutableInteractionSource() },
                                            ) {
                                                lastInteraction = System.currentTimeMillis()
                                                entryPressed = true
                                                viewModel.select("ENTRY", memo)
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        // 채워지는 배경
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(entryFill)
                                                .fillMaxHeight()
                                                .align(Alignment.CenterStart)
                                                .background(TealPrimary),
                                        )
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                        ) {
                                            Image(
                                                painter = painterResource(R.drawable.ic_drawer_lightning),
                                                contentDescription = null,
                                                modifier = Modifier.size(40.dp),
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "출근",
                                                fontSize = 36.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (entryFill > 0.5f) Color.White else TealPrimary,
                                            )
                                        }
                                    }

                                    // 퇴근 버튼
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(140.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(RedBg)
                                            .border(2.dp, RedButton, RoundedCornerShape(20.dp))
                                            .clickable(
                                                indication = null,
                                                interactionSource = remember { MutableInteractionSource() },
                                            ) {
                                                lastInteraction = System.currentTimeMillis()
                                                exitPressed = true
                                                viewModel.select("EXIT", memo)
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        // 채워지는 배경
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(exitFill)
                                                .fillMaxHeight()
                                                .align(Alignment.CenterStart)
                                                .background(RedButton),
                                        )
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                        ) {
                                            Image(
                                                painter = painterResource(R.drawable.ic_drawer_logout),
                                                contentDescription = null,
                                                modifier = Modifier.size(40.dp),
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "퇴근",
                                                fontSize = 36.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (exitFill > 0.5f) Color.White else RedButton,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 메모 입력 (카드 아래)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(CardBg)
                                .padding(horizontal = 24.dp, vertical = 20.dp),
                        ) {
                            if (memo.isEmpty()) {
                                Text(
                                    text = "메모 입력 (선택)",
                                    fontSize = 22.sp,
                                    color = ScreenGrayText,
                                )
                            }
                            BasicTextField(
                                value = memo,
                                onValueChange = {
                                    if (it.length <= 500) {
                                        memo = it
                                        lastInteraction = System.currentTimeMillis()
                                    }
                                },
                                textStyle = TextStyle(
                                    fontSize = 22.sp,
                                    color = Color.White,
                                ),
                                cursorBrush = SolidColor(TealPrimary),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
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
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xCC333333))
                    .padding(horizontal = 32.dp, vertical = 20.dp),
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
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(TealPrimary),
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

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = Color(0xFFE53935),
                contentColor = Color.White,
            )
        }
    }
}

