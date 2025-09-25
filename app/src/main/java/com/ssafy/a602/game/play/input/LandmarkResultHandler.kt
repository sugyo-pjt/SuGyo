package com.ssafy.a602.game.play.input

import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class LandmarkResultHandler(
    private val buffer: DynamicLandmarkBuffer,
    private val mirrorCompensation: Boolean = false, // 전면 미러링 입력으로 추론했다면 true
    private val onLandmarks: ((List<LM>, List<LM>, List<LM>) -> Unit)? = null // 🔥 ViewModel 콜백
) {
    private var currentPoseResult: PoseLandmarkerResult? = null
    private var currentHandResult: HandLandmarkerResult? = null
    private var lastTimestamp: Long = 0L
    private var lastCoordinateLogTime: Long = 0L

    fun onPoseResult(result: PoseLandmarkerResult, tsMs: Long) {
        android.util.Log.d("LandmarkResultHandler", "Pose 결과 수신: ${result.landmarks().size}개 포즈 감지")
        currentPoseResult = result
        lastTimestamp = tsMs
        processResults()
    }

    fun onHandResult(result: HandLandmarkerResult, tsMs: Long) {
        val landmarks = result.landmarks()
        val handedness = result.handednesses()
        
        android.util.Log.d("LandmarkResultHandler", "Hand 결과 수신: ${landmarks.size}개 손 감지")
        
        // 손 인식 상세 로그
        if (landmarks.isEmpty()) {
            android.util.Log.w("LandmarkResultHandler", "⚠️ 손이 인식되지 않음 - 카메라에 손이 보이는지 확인하세요")
        } else {
            android.util.Log.d("LandmarkResultHandler", "✅ 손 인식 성공: ${landmarks.size}개")
            landmarks.forEachIndexed { i, hand ->
                val handLabel = handedness.getOrNull(i)?.firstOrNull()?.categoryName() ?: "Unknown"
                android.util.Log.d("LandmarkResultHandler", "  손 $i: $handLabel, 랜드마크 ${hand.size}개")
            }
        }
        
        currentHandResult = result
        lastTimestamp = tsMs
        processResults()
    }

    private fun processResults() {
        val poseResult = currentPoseResult
        val handResult = currentHandResult

        if (poseResult == null && handResult == null) return

        // Pose (필요 시 유지)
        val poseSrc = poseResult?.landmarks()?.firstOrNull() ?: emptyList()

        // ★ Hand: handedness 기반으로 좌우 분리
        var leftSrc: List<NormalizedLandmark> = emptyList()
        var rightSrc: List<NormalizedLandmark> = emptyList()

        if (handResult != null && handResult.landmarks().isNotEmpty()) {
            val hands = handResult.landmarks()
            val handedness = handResult.handednesses() // List<List<Category>>

            for (i in hands.indices) {
                val lm = hands[i]
                val label = handedness.getOrNull(i)?.firstOrNull()?.categoryName() ?: "Unknown"

                // 미러 입력으로 추론했다면 좌우를 보정
                val finalLabel = if (mirrorCompensation) {
                    when (label) {
                        "Left" -> "Right"
                        "Right" -> "Left"
                        else -> label
                    }
                } else label

                when (finalLabel) {
                    "Left"  -> leftSrc = lm
                    "Right" -> rightSrc = lm
                    else -> {
                        // fallback: 아무 라벨도 없는 경우엔 x로만 대충 나눔 (최후의 수단)
                        val cx = lm.map { it.x() }.average()
                        if (cx >= 0.5) leftSrc = lm else rightSrc = lm
                    }
                }
            }
        }

        // 손 인식 상태 상세 로그
        val handDetectionStatus = when {
            handResult == null -> "손 결과 없음"
            handResult.landmarks().isEmpty() -> "손 감지 실패"
            else -> "손 감지 성공 (${handResult.landmarks().size}개)"
        }
        
        android.util.Log.d("LandmarkResultHandler",
            "랜드마크 추출 완료 - 포즈: ${poseSrc.size}, 왼손: ${leftSrc.size}, 오른손: ${rightSrc.size}")
        android.util.Log.d("LandmarkResultHandler", "손 인식 상태: $handDetectionStatus")

        val pose = toLMFiltered(poseSrc, POSE_KEEP)
        val left = toLMFiltered(leftSrc, HAND_KEEP)
        val right = toLMFiltered(rightSrc, HAND_KEEP)

        buffer.add(FramePack(lastTimestamp, pose, left, right))
        
        // 🔥 ViewModel에 랜드마크 결과 전달 (null 제거)
        val finalPose = pose.filterNotNull()
        val finalLeft = left.filterNotNull()
        val finalRight = right.filterNotNull()
        
        android.util.Log.d("LandmarkResultHandler", "🎯 ViewModel 콜백 호출: pose=${finalPose.size}, left=${finalLeft.size}, right=${finalRight.size}")
        
        onLandmarks?.invoke(finalPose, finalLeft, finalRight)

        currentPoseResult = null
        currentHandResult = null
    }
}
