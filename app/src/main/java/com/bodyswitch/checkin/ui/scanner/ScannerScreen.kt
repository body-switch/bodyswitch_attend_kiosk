package com.bodyswitch.checkin.ui.scanner

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bodyswitch.checkin.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

private val DarkBg = Color(0xFF000000)
private val TealPrimary = Color(0xFF4AB3BC)
private val TealCard = Color(0x524AB3BC)
private val PanelBg = Color(0x14FFFFFF)
private val GrayText = Color(0xFFBFBFBF)
private val CameraAreaBg = Color(0xFF1E1E1E)
private val Red = Color(0xFFE53935)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    onQrScanned: (String) -> Unit,
    viewModel: ScannerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

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

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // 카메라 권한 자동 요청
    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 100.dp), // 상단 바 공간
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // QR 체크인 타이틀
            Text(
                text = "QR 체크인",
                fontSize = 36.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "앱의 QR코드를 스캐너에 비춰주세요",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = GrayText,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 카메라 영역
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .weight(1f)
                    .padding(bottom = 24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(CameraAreaBg),
                contentAlignment = Alignment.Center,
            ) {
                if (cameraPermission.status.isGranted) {
                    // 카메라 프리뷰
                    CameraPreview(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(24.dp)),
                        onQrDetected = { qrData ->
                            viewModel.onQrDetected(qrData, onQrScanned)
                        },
                    )
                } else {
                    // 카메라 권한 없을 때
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = GrayText,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "카메라 권한을 허용해주세요",
                            color = GrayText,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { cameraPermission.launchPermissionRequest() },
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("권한 허용", fontSize = 16.sp)
                        }
                    }
                }

                // QR 인식 범위 바깥을 어둡게 가리는 오버레이
                val scanSize = 260.dp
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
                    // QR 스캔 프레임 아이콘
                    Image(
                        painter = painterResource(R.drawable.ic_qr_scan_frame),
                        contentDescription = null,
                        modifier = Modifier
                            .size(scanSize)
                            .align(Alignment.Center),
                    )
                }
            }

            // 하단 안내 텍스트
            Text(
                text = "도움이 필요하시면 프론트에 문의해 주세요",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = TealPrimary,
            )

            Spacer(modifier = Modifier.height(32.dp))
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

            // 센터명
            Text(
                text = "바디스위치 피트니스",
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

@Preview(
    name = "QR 체크인 화면",
    widthDp = 1920,
    heightDp = 1080,
    showBackground = true,
    backgroundColor = 0xFF000000,
)
@Composable
private fun ScannerScreenPreview() {
    ScannerScreen(onQrScanned = {})
}
