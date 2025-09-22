package com.ssafy.a602.game.play

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.ssafy.a602.game.play.input.LandmarkResultHandler
import com.ssafy.a602.game.play.input.WordWindowUploader
import java.util.concurrent.TimeUnit

/**
 * GamePlayCamera
 * 
 * MediaPipe Pose Landmarker와 Hand Landmarker를 사용하여 실시간 수어 인식을 수행하는 카메라 클래스
 * 카메라 프레임을 분석하여 포즈와 손 랜드마크를 추출하고 서버로 전송
 */
@ExperimentalGetImage
class GamePlayCamera(
    private val resultHandler: LandmarkResultHandler,
    private val uploader: WordWindowUploader
) {
    private var poseLandmarker: PoseLandmarker? = null
    private var handLandmarker: HandLandmarker? = null
    private var rgb: Bitmap? = null
    private lateinit var yuv2rgb: YuvToRgbConverter

    /**
     * MediaPipe Pose Landmarker와 Hand Landmarker 초기화
     */
    fun init(context: Context) {
        yuv2rgb = YuvToRgbConverter(context)

        try {
            android.util.Log.d("GamePlayCamera", "MediaPipe 모델 초기화 시작")
            
            // Pose Landmarker 초기화
            android.util.Log.d("GamePlayCamera", "Pose Landmarker 모델 로드: models/pose_landmarker_lite.task")
            val poseBaseOptions = BaseOptions.builder()
                .setModelAssetPath("models/pose_landmarker_lite.task")
                .build()
            
            val poseOptions = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(poseBaseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener { result, image ->
                    val timestampMs = System.currentTimeMillis()
                    resultHandler.onPoseResult(result, timestampMs)
                }
                .build()
            
            poseLandmarker = PoseLandmarker.createFromOptions(context, poseOptions)
            android.util.Log.d("GamePlayCamera", "Pose Landmarker 초기화 완료")

            // Hand Landmarker 초기화
            android.util.Log.d("GamePlayCamera", "Hand Landmarker 모델 로드: models/hand_landmarker.task")
            val handBaseOptions = BaseOptions.builder()
                .setModelAssetPath("models/hand_landmarker.task")
                .build()
            
            val handOptions = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(handBaseOptions)
                .setNumHands(2) // ★ 두 손 감지
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener { result, image ->
                    val timestampMs = System.currentTimeMillis()
                    resultHandler.onHandResult(result, timestampMs)
                    uploader.maybeFlush()
                }
                .build()
            
            handLandmarker = HandLandmarker.createFromOptions(context, handOptions)
            android.util.Log.d("GamePlayCamera", "Hand Landmarker 초기화 완료")
            android.util.Log.d("GamePlayCamera", "MediaPipe 모델 초기화 완료")
        } catch (e: Exception) {
            android.util.Log.e("GamePlayCamera", "MediaPipe 초기화 실패: ${e.message}", e)
            throw RuntimeException("MediaPipe 초기화에 실패했습니다. pose_landmarker_lite.task와 hand_landmarker.task 파일을 확인해주세요.", e)
        }
    }

    /**
     * CameraX ImageAnalysis.Analyzer
     * 카메라 프레임을 받아서 MediaPipe로 분석
     */
    val analyzer = ImageAnalysis.Analyzer { image: ImageProxy ->
        val media = image.image ?: return@Analyzer
        
        // Bitmap 크기 조정 (필요시)
        if (rgb == null || rgb!!.width != image.width || rgb!!.height != image.height) {
            rgb = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
        }
        
        // YUV → RGB 변환
        yuv2rgb.yuvToRgb(media, rgb!!)
        
        // MediaPipe 이미지 생성
        val mpImage = BitmapImageBuilder(rgb!!).build()
        val timestampMs = TimeUnit.NANOSECONDS.toMillis(image.imageInfo.timestamp)
        
        // MediaPipe 분석 실행
        poseLandmarker?.detectAsync(mpImage, timestampMs)
        handLandmarker?.detectAsync(mpImage, timestampMs)
        
        // 디버그: 분석 실행 확인 (너무 많은 로그를 방지하기 위해 30프레임마다)
        if (timestampMs % 1000 < 33) { // 약 1초마다
            android.util.Log.d("GamePlayCamera", "MediaPipe 분석 실행 중... timestamp: $timestampMs")
        }
        
        // image.close()는 CameraPreview에서 자동으로 호출됨
    }

    /**
     * 리소스 해제
     */
    fun release() {
        poseLandmarker?.close()
        handLandmarker?.close()
        poseLandmarker = null
        handLandmarker = null
    }
}