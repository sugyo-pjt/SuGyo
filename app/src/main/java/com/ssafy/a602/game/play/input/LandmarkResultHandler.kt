package com.ssafy.a602.game.play.input

import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class LandmarkResultHandler(
    private val buffer: LandmarkBuffer3s,
    private val mirrorCompensation: Boolean = false // 전면 미러링 입력으로 추론했다면 true
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
        android.util.Log.d("LandmarkResultHandler", "Hand 결과 수신: ${result.landmarks().size}개 손 감지")
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

        android.util.Log.d("LandmarkResultHandler",
            "랜드마크 추출 완료 - 포즈: ${poseSrc.size}, 왼손: ${leftSrc.size}, 오른손: ${rightSrc.size}")

        val pose = toLMFiltered(poseSrc, POSE_KEEP)
        val left = toLMFiltered(leftSrc, HAND_KEEP)
        val right = toLMFiltered(rightSrc, HAND_KEEP)

        buffer.add(FramePack(lastTimestamp, pose, left, right))

        currentPoseResult = null
        currentHandResult = null
    }
}
