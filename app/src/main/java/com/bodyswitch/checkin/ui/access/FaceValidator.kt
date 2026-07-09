package com.bodyswitch.checkin.ui.access

import android.graphics.Rect
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import java.util.concurrent.atomic.AtomicBoolean

// 안면 촬영 프레임을 실시간 분석해 UBio 품질체크를 통과할 만한 사진인지 판정한다.
// 임계값은 AOS(회원앱) FaceDistanceDetector와 동일 — AOS로 등록한 사진이 UBio 품질체크를 통과하므로 같은 기준을 쓴다.
data class FaceValidationResult(
    val isValid: Boolean,
    val feedbackMessage: String,
)

class FaceValidator(
    private val onResult: (FaceValidationResult) -> Unit,
) : ImageAnalysis.Analyzer {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.25f)
            .build(),
    )

    private val isClosed = AtomicBoolean(false)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (isClosed.get()) {
            imageProxy.close()
            return
        }
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (!isClosed.get()) {
                    onResult(evaluate(faces, imageProxy.width, imageProxy.height))
                }
            }
            .addOnFailureListener {
                if (!isClosed.get()) {
                    onResult(FaceValidationResult(false, FEEDBACK_NO_FACE))
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun evaluate(faces: List<Face>, imageWidth: Int, imageHeight: Int): FaceValidationResult {
        if (faces.isEmpty()) return FaceValidationResult(false, FEEDBACK_NO_FACE)
        if (faces.size > 1) return FaceValidationResult(false, FEEDBACK_MULTI_FACE)

        val face = faces[0]

        val distance = analyzeDistance(face.boundingBox, imageWidth, imageHeight)
        if (!distance.first) return FaceValidationResult(false, distance.second)

        val frontal = analyzeFrontal(face)
        if (!frontal.first) return FaceValidationResult(false, frontal.second)

        if (!hasAllLandmarks(face)) return FaceValidationResult(false, FEEDBACK_NO_FACE)

        return FaceValidationResult(true, FEEDBACK_OK)
    }

    // 얼굴이 이미지의 MIN~MAX 비율을 차지하는지 (너무 멀거나 가깝지 않은지)
    private fun analyzeDistance(box: Rect, imageWidth: Int, imageHeight: Int): Pair<Boolean, String> {
        val widthRatio = box.width().toFloat() / imageWidth
        val heightRatio = box.height().toFloat() / imageHeight
        val maxRatio = maxOf(widthRatio, heightRatio)
        val minRatio = minOf(widthRatio, heightRatio)
        return when {
            maxRatio > MAX_FACE_RATIO -> false to FEEDBACK_TOO_CLOSE
            minRatio < MIN_FACE_RATIO -> false to FEEDBACK_TOO_FAR
            else -> true to FEEDBACK_OK
        }
    }

    // 정면 판별 (오일러 각 ±20°)
    private fun analyzeFrontal(face: Face): Pair<Boolean, String> = when {
        kotlin.math.abs(face.headEulerAngleY) > MAX_EULER_ANGLE -> false to FEEDBACK_LOOK_FRONT
        kotlin.math.abs(face.headEulerAngleX) > MAX_EULER_ANGLE -> false to FEEDBACK_NO_TILT_UPDOWN
        kotlin.math.abs(face.headEulerAngleZ) > MAX_EULER_ANGLE -> false to FEEDBACK_NO_TILT_SIDE
        else -> true to FEEDBACK_OK
    }

    // 양쪽 눈·코·입 랜드마크가 모두 검출됐는지 (측면/가림 방지)
    private fun hasAllLandmarks(face: Face): Boolean {
        val hasLeftEye = face.getLandmark(FaceLandmark.LEFT_EYE) != null
        val hasRightEye = face.getLandmark(FaceLandmark.RIGHT_EYE) != null
        val hasNose = face.getLandmark(FaceLandmark.NOSE_BASE) != null
        val hasMouth = face.getLandmark(FaceLandmark.MOUTH_BOTTOM) != null ||
            face.getLandmark(FaceLandmark.MOUTH_LEFT) != null ||
            face.getLandmark(FaceLandmark.MOUTH_RIGHT) != null
        return hasLeftEye && hasRightEye && hasNose && hasMouth
    }

    fun close() {
        isClosed.set(true)
        detector.close()
    }

    companion object {
        // AOS FaceDistanceDetector와 동일한 임계값
        private const val MIN_FACE_RATIO = 0.28f
        private const val MAX_FACE_RATIO = 0.50f
        private const val MAX_EULER_ANGLE = 20f

        private const val FEEDBACK_OK = ""
        private const val FEEDBACK_NO_FACE = "얼굴을 카메라에 비춰주세요"
        private const val FEEDBACK_MULTI_FACE = "한 명만 촬영해주세요"
        private const val FEEDBACK_TOO_CLOSE = "조금만 멀어져 주세요"
        private const val FEEDBACK_TOO_FAR = "조금만 가까이 와주세요"
        private const val FEEDBACK_LOOK_FRONT = "정면을 바라봐 주세요"
        private const val FEEDBACK_NO_TILT_UPDOWN = "고개를 들거나 숙이지 마세요"
        private const val FEEDBACK_NO_TILT_SIDE = "고개를 기울이지 마세요"
    }
}
