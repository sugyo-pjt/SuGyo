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
    private val uploader: WordWindowUploader?
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
            android.util.Log.d("GamePlayCamera", "MediaPipe 모델 초기화 시작 (점진적 로딩)")
            
            // 1단계: Pose Landmarker 먼저 초기화 (더 가벼움)
            android.util.Log.d("GamePlayCamera", "1단계: Pose Landmarker 모델 로드 시작")
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
            android.util.Log.d("GamePlayCamera", "✅ 1단계: Pose Landmarker 초기화 완료")

            // 2단계: Hand Landmarker 초기화 (더 무거움)
            android.util.Log.d("GamePlayCamera", "2단계: Hand Landmarker 모델 로드 시작")
            val handBaseOptions = BaseOptions.builder()
                .setModelAssetPath("models/hand_landmarker.task")
                .build()
            
            val handOptions = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(handBaseOptions)
                .setNumHands(2) // ★ 두 손 감지
                .setMinHandDetectionConfidence(0.3f) // 손 감지 최소 신뢰도 (낮춤)
                .setMinHandPresenceConfidence(0.3f) // 손 존재 최소 신뢰도 (낮춤)
                .setMinTrackingConfidence(0.3f) // 추적 최소 신뢰도 (낮춤)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener { result, image ->
                    val timestampMs = System.currentTimeMillis()
                    resultHandler.onHandResult(result, timestampMs)
                    uploader?.maybeFlush()
                }
                .build()
            
            handLandmarker = HandLandmarker.createFromOptions(context, handOptions)
            android.util.Log.d("GamePlayCamera", "✅ 2단계: Hand Landmarker 초기화 완료")
            android.util.Log.d("GamePlayCamera", "🎉 MediaPipe 모델 초기화 완료 (점진적 로딩)")
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
        try {
            val media = image.image ?: return@Analyzer
            
            // 이미지 유효성 검사
            if (image.width <= 0 || image.height <= 0) {
                android.util.Log.w("GamePlayCamera", "유효하지 않은 이미지 크기: ${image.width}x${image.height}")
                return@Analyzer
            }
            
            // Bitmap 크기 조정 (필요시) - 메모리 누수 방지를 위한 안전한 처리
            if (rgb == null || rgb!!.width != image.width || rgb!!.height != image.height) {
                try {
                    rgb?.recycle() // 기존 Bitmap 메모리 해제
                    rgb = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
                    android.util.Log.d("GamePlayCamera", "Bitmap 크기 조정: ${image.width}x${image.height}")
                } catch (e: OutOfMemoryError) {
                    android.util.Log.e("GamePlayCamera", "Bitmap 생성 실패 - 메모리 부족: ${e.message}")
                    return@Analyzer
                } catch (e: Exception) {
                    android.util.Log.e("GamePlayCamera", "Bitmap 생성 실패: ${e.message}")
                    return@Analyzer
                }
            }
            
            // YUV → RGB 변환 - 안전한 처리
            try {
                yuv2rgb.yuvToRgb(media, rgb!!)
            } catch (e: Exception) {
                android.util.Log.e("GamePlayCamera", "YUV to RGB 변환 실패: ${e.message}")
                return@Analyzer
            }
            
            // MediaPipe 이미지 생성 - 안전한 처리
            val mpImage = try {
                BitmapImageBuilder(rgb!!).build()
            } catch (e: Exception) {
                android.util.Log.e("GamePlayCamera", "MediaPipe 이미지 생성 실패: ${e.message}")
                return@Analyzer
            }
            
            val timestampMs = TimeUnit.NANOSECONDS.toMillis(image.imageInfo.timestamp)
            
            // 손 인식을 위한 이미지 품질 확인
            if (timestampMs % 2000 < 33) { // 2초마다 한 번
                android.util.Log.d("GamePlayCamera", "이미지 품질 확인: ${rgb!!.width}x${rgb!!.height}, 포맷: ${rgb!!.config}")
            }
            
            // MediaPipe 분석 실행 - 안전한 처리
            try {
                poseLandmarker?.detectAsync(mpImage, timestampMs)
                handLandmarker?.detectAsync(mpImage, timestampMs)
            } catch (e: Exception) {
                android.util.Log.e("GamePlayCamera", "MediaPipe 분석 실행 실패: ${e.message}")
                // 분석 실패해도 앱이 크래시되지 않도록 계속 진행
            }
            
            // 디버그: 분석 실행 확인 (너무 많은 로그를 방지하기 위해 30프레임마다)
            if (timestampMs % 1000 < 33) { // 약 1초마다
                android.util.Log.d("GamePlayCamera", "MediaPipe 분석 실행 중... timestamp: $timestampMs")
                android.util.Log.d("GamePlayCamera", "  - Pose Landmarker: ${if (poseLandmarker != null) "활성" else "비활성"}")
                android.util.Log.d("GamePlayCamera", "  - Hand Landmarker: ${if (handLandmarker != null) "활성" else "비활성"}")
            }
            
        } catch (e: Exception) {
            android.util.Log.e("GamePlayCamera", "Analyzer 처리 중 오류: ${e.message}", e)
            // 예외가 발생해도 앱이 크래시되지 않도록 처리
        } finally {
            // image.close()는 CameraPreview에서 자동으로 호출됨
        }
    }

    /**
     * 리소스 해제
     */
    fun release() {
        try {
            poseLandmarker?.close()
            handLandmarker?.close()
        } catch (e: Exception) {
            android.util.Log.e("GamePlayCamera", "MediaPipe 리소스 해제 중 오류: ${e.message}")
        } finally {
            poseLandmarker = null
            handLandmarker = null
            // Bitmap 메모리 해제
            rgb?.recycle()
            rgb = null
        }
    }
}