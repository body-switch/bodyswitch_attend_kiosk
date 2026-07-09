package com.bodyswitch.checkin.ui.access

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Base64
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

// 안면등록용 촬영 이미지 인코딩.
// 회전(EXIF) 보정 + 전면 카메라 미러링 보정 → 긴 변 1280px 다운스케일 → JPEG 85 → Base64.
// 과압축은 UBio 품질체크 탈락을 유발하므로 품질을 더 낮추지 않는다.
object FaceImageEncoder {

    private const val MAX_LONG_SIDE = 1280
    private const val JPEG_QUALITY = 85

    fun encodeToBase64Jpeg(image: ImageProxy): String {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }

        // 1) 바운즈만 먼저 읽어 원본 해상도 확인 (MAXIMIZE_QUALITY 캡처는 풀센서 = 최대 수천만 픽셀)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        // 2) 디코드 단계에서 다운샘플 → 풀해상도 ARGB_8888(수십 MB) 할당 자체를 회피 (OOM 예방)
        //    긴 변이 목표(1280) 이상은 유지하도록 2의 거듭제곱 샘플링만 적용 (품질 손실 없음)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = computeInSampleSize(maxOf(bounds.outWidth, bounds.outHeight), MAX_LONG_SIDE)
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

        val longSide = maxOf(upright.width, upright.height)
        val scaled = if (longSide > MAX_LONG_SIDE) {
            val scale = MAX_LONG_SIDE.toFloat() / longSide
            val resized = Bitmap.createScaledBitmap(
                upright,
                (upright.width * scale).toInt(),
                (upright.height * scale).toInt(),
                true,
            )
            if (resized != upright) upright.recycle()
            resized
        } else {
            upright
        }

        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
        scaled.recycle()

        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    // 긴 변이 target 이상으로 남는 최대 2의 거듭제곱 샘플링 값 (이후 정확히 1280으로 스케일)
    private fun computeInSampleSize(longSide: Int, target: Int): Int {
        var sample = 1
        while (longSide / (sample * 2) >= target) sample *= 2
        return sample
    }
}
