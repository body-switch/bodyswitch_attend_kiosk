package com.bodyswitch.checkin.ui.scanner

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

data class ScannerUiState(
    val isScanning: Boolean = true,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class ScannerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private var lastScannedValue: String? = null
    private var lastScannedTime: Long = 0

    fun onQrDetected(qrData: String, onSuccess: (String) -> Unit) {
        // 같은 QR 연속 스캔 방지 (2초 쿨다운)
        val now = System.currentTimeMillis()
        if (qrData == lastScannedValue && now - lastScannedTime < 2000) return

        lastScannedValue = qrData
        lastScannedTime = now

        Log.d("QR_DATA", "=== QR 스캔 데이터 ===")
        Log.d("QR_DATA", "원본: $qrData")
        Log.d("QR_DATA", "길이: ${qrData.length}")
        Log.d("QR_DATA", "========================")

        vibrate()
        onSuccess(qrData)
    }

    fun onImageSelected(uri: Uri, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, errorMessage = null)

            val result = scanQrFromImage(context, uri)

            if (result != null) {
                vibrate()
                _uiState.value = _uiState.value.copy(isProcessing = false)
                onSuccess(result)
            } else {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMessage = "QR 코드를 찾을 수 없습니다",
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        vibrator.vibrate(
            VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    }
}
