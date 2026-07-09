package com.bodyswitch.checkin.ui.access

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

// 안면 촬영용 전면 카메라 프리뷰. 촬영용 ImageCapture와 함께
// 실시간 얼굴 검증용 ImageAnalysis(FaceValidator)를 같이 바인딩한다.
@Composable
fun FaceCameraPreview(
    imageCapture: ImageCapture,
    onFaceResult: (FaceValidationResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnFaceResult by rememberUpdatedState(onFaceResult)

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val disposed = AtomicBoolean(false)
            val analysisExecutor = Executors.newSingleThreadExecutor()
            var validator: FaceValidator? = null

            fun bindCamera() {
                if (disposed.get()) return
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    if (disposed.get()) return@addListener
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        cameraProvider.unbindAll()
                        // 재바인드(resume) 시 이전 detector 네이티브 리소스 해제 (누수 방지)
                        validator?.close()

                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val faceValidator = FaceValidator { result -> currentOnFaceResult(result) }
                        validator = faceValidator

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { it.setAnalyzer(analysisExecutor, faceValidator) }

                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_FRONT_CAMERA,
                            preview,
                            imageCapture,
                            imageAnalysis,
                        )
                    } catch (e: Exception) {
                        Log.e("FaceCameraPreview", "카메라 바인딩 실패", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))
            }

            fun unbindCamera() {
                try {
                    val future = ProcessCameraProvider.getInstance(ctx)
                    if (future.isDone) {
                        future.get().unbindAll()
                    }
                } catch (e: Exception) {
                    Log.e("FaceCameraPreview", "카메라 언바인딩 실패", e)
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

            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                bindCamera()
            }

            previewView.addOnAttachStateChangeListener(object : android.view.View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: android.view.View) {}
                override fun onViewDetachedFromWindow(v: android.view.View) {
                    disposed.set(true)
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    unbindCamera()
                    validator?.close()
                    analysisExecutor.shutdown()
                }
            })

            previewView
        },
    )
}
