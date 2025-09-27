package com.ssafy.a602.game.play

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * YuvToRgbConverter
 * 
 * CameraX의 ImageProxy에서 받은 YUV 이미지를 RGB Bitmap으로 변환하는 유틸리티 클래스
 * MediaPipe에서 사용할 수 있는 형태로 이미지를 변환
 */
class YuvToRgbConverter(private val context: Context) {

    /**
     * YUV 이미지를 RGB Bitmap으로 변환
     * 
     * @param image CameraX ImageProxy의 image
     * @param outputBitmap 변환된 RGB 데이터를 저장할 Bitmap
     */
    fun yuvToRgb(image: Image, outputBitmap: Bitmap) {
        val planes = image.planes
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        // Y plane
        yBuffer.get(nv21, 0, ySize)

        // U and V planes (interleaved)
        val uvPixel = vSize / (image.width / 2)
        for (i in 0 until uvPixel) {
            for (j in 0 until image.width / 2) {
                val uvIndex = i * image.width + j * 2
                nv21[ySize + uvIndex] = vBuffer.get()
                nv21[ySize + uvIndex + 1] = uBuffer.get()
            }
        }

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, out)
        val imageBytes = out.toByteArray()
        
        // Bitmap으로 변환
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        
        // 출력 Bitmap에 복사
        val canvas = android.graphics.Canvas(outputBitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
    }
}
