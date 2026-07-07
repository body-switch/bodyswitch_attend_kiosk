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

        val source = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
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
}
