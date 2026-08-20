package com.bodyswitch.checkin.ui.access

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Base64
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

// 안면등록용 촬영 이미지 인코딩.
// 회전(EXIF) 보정 + 전면 카메라 미러링 보정 → 얼굴 중심 60% 크롭 → 긴 변 1280px → JPEG 85 → Base64.
//
// 크롭이 핵심이다. 회원앱(AOS) FaceCameraActivity 가 촬영본에서 얼굴을 다시 검출해
// 얼굴 중심 60%(=1.67배 확대)만 올리고, 그 사진이 UBio 품질체크를 통과한다.
// 전체 프레임을 그대로 축소해 올리면 얼굴 절대 픽셀이 작아 ERROR_FACEWT_SMALL 로 떨어진다
// (2026-08-19 운영 실측). 과압축도 같은 결과를 부르므로 품질을 더 낮추지 않는다.
object FaceImageEncoder {

    private const val MAX_LONG_SIDE = 1280
    private const val JPEG_QUALITY = 85

    // AOS 와 동일한 크롭 비율. 0.6 = 가운데 60% 영역만 사용 = 1.67배 확대 효과
    private const val CROP_RATIO = 0.6f
    private const val FACE_DETECT_TIMEOUT_SECONDS = 3L

    fun encodeToBase64Jpeg(image: ImageProxy): String {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }

        // 1) 바운즈만 먼저 읽어 원본 해상도 확인 (풀센서 캡처는 수천만 픽셀)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        // 2) 디코드 단계에서 다운샘플 → 풀해상도 ARGB_8888(수십 MB) 할당 자체를 회피 (OOM 예방)
        //    크롭 후에도 목표 해상도가 남아야 하므로 목표를 CROP_RATIO 만큼 키워 잡는다
        val decodeTarget = (MAX_LONG_SIDE / CROP_RATIO).toInt()
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = computeInSampleSize(maxOf(bounds.outWidth, bounds.outHeight), decodeTarget)
        }
        val source = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
            ?: throw IllegalStateException("촬영 이미지 디코딩 실패")

        // rotationDegrees = 업라이트 표시에 필요한 회전량 (JPEG 버퍼의 EXIF는 decode 시 무시됨)
        val matrix = Matrix().apply {
            postRotate(image.imageInfo.rotationDegrees.toFloat())
            // 전면 카메라: 프리뷰(거울상)와 일치하도록 좌우 반전
            postScale(-1f, 1f)
        }
        val upright = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (upright != source) source.recycle()

        // 3) 촬영본에서 얼굴을 다시 검출해 그 중심으로 크롭한다.
        //    실시간 검증(FaceValidator)은 분석 프레임 기준이라 촬영본의 얼굴 위치와 다를 수 있다.
        val cropped = cropAroundFace(upright, detectFaceCenter(upright))
        if (cropped != upright) upright.recycle()

        val longSide = maxOf(cropped.width, cropped.height)
        val scaled = if (longSide > MAX_LONG_SIDE) {
            val scale = MAX_LONG_SIDE.toFloat() / longSide
            val resized = Bitmap.createScaledBitmap(
                cropped,
                (cropped.width * scale).toInt(),
                (cropped.height * scale).toInt(),
                true,
            )
            if (resized != cropped) cropped.recycle()
            resized
        } else {
            cropped
        }

        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
        scaled.recycle()

        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    // 얼굴 중심 좌표 비율(0.0~1.0). 검출 실패·타임아웃이면 이미지 중앙으로 폴백한다(AOS 동일).
    private fun detectFaceCenter(bitmap: Bitmap): Pair<Float, Float> {
        val detector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build(),
        )
        return try {
            val faces = Tasks.await(
                detector.process(InputImage.fromBitmap(bitmap, 0)),
                FACE_DETECT_TIMEOUT_SECONDS,
                TimeUnit.SECONDS,
            )
            val box = faces.firstOrNull()?.boundingBox ?: return CENTER
            box.centerX().toFloat() / bitmap.width to box.centerY().toFloat() / bitmap.height
        } catch (_: Exception) {
            CENTER
        } finally {
            detector.close()
        }
    }

    // 얼굴 중심을 크롭 영역의 중심에 두되 이미지 밖으로 나가지 않게 clamp
    private fun cropAroundFace(bitmap: Bitmap, center: Pair<Float, Float>): Bitmap {
        val newWidth = (bitmap.width * CROP_RATIO).toInt()
        val newHeight = (bitmap.height * CROP_RATIO).toInt()
        val x = ((bitmap.width * center.first).toInt() - newWidth / 2)
            .coerceIn(0, bitmap.width - newWidth)
        val y = ((bitmap.height * center.second).toInt() - newHeight / 2)
            .coerceIn(0, bitmap.height - newHeight)
        return Bitmap.createBitmap(bitmap, x, y, newWidth, newHeight)
    }

    // 긴 변이 target 이상으로 남는 최대 2의 거듭제곱 샘플링 값
    private fun computeInSampleSize(longSide: Int, target: Int): Int {
        var sample = 1
        while (longSide / (sample * 2) >= target) sample *= 2
        return sample
    }

    private val CENTER = 0.5f to 0.5f
}
