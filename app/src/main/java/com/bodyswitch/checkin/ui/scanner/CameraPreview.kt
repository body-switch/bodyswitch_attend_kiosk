package com.bodyswitch.checkin.ui.scanner

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onQrDetected: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnQrDetected by rememberUpdatedState(onQrDetected)

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = Executors.newSingleThreadExecutor()
            val disposed = AtomicBoolean(false)
            var analyzer: QrAnalyzer? = null

            fun bindCamera() {
                if (disposed.get()) return
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    if (disposed.get()) return@addListener
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        cameraProvider.unbindAll()

                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val qrAnalyzer = QrAnalyzer { qr -> currentOnQrDetected(qr) }
                        analyzer = qrAnalyzer

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(executor, qrAnalyzer)
                            }

                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_FRONT_CAMERA,
                            preview,
                            imageAnalysis,
                        )
                    } catch (e: Exception) {
                        Log.e("CameraPreview", "카메라 바인딩 실패", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))
            }

            fun unbindCamera() {
                analyzer?.close()
                analyzer = null
                try {
                    val future = ProcessCameraProvider.getInstance(ctx)
                    if (future.isDone) {
                        future.get().unbindAll()
                    }
                } catch (e: Exception) {
                    Log.e("CameraPreview", "카메라 언바인딩 실패", e)
                }
            }

            // Lifecycle observer: RESUMED에서 바인드, PAUSED에서 언바인드
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> bindCamera()
                    Lifecycle.Event.ON_PAUSE -> unbindCamera()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)

            // 현재 이미 RESUMED 상태면 즉시 바인드
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                bindCamera()
            }

            // View가 detach될 때 정리
            previewView.addOnAttachStateChangeListener(object : android.view.View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: android.view.View) {}
                override fun onViewDetachedFromWindow(v: android.view.View) {
                    disposed.set(true)
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    unbindCamera()
                    executor.shutdownNow()
                }
            })

            previewView
        },
    )
}
