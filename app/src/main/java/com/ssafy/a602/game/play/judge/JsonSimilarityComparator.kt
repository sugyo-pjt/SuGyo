package com.ssafy.a602.game.play.judge

import kotlin.math.max

/**
 * 동작 유사도 비교기
 * 
 * 동작 과정:
 * JSON → MotionFrame → HandNormalization → 특징벡터 → 코사인유사도
 */
class JsonSimilarityComparator private constructor() {
    
    companion object {
        /**
         * 0.3초 프레임 전부에 대한 최고 유사도 계산
         */
        @JvmStatic
        fun calculateMotionSimilarity(againstChart: List<MotionFrame>, correctChart: List<MotionFrame>, width: Int, height: Int): Double {
            if (againstChart.isEmpty() || correctChart.isEmpty()) return 0.0
            var best = 0.0
            for (i in againstChart.indices) {
                for (j in correctChart.indices) {
                    val s = calculateFrameSimilarity(againstChart[i], correctChart[j], width, height)
                    best = max(best, s)
                }
            }
            return best
        }

        /**
         * 각 프레임 별 유사도 계산 - 자바 서버 코드와 일치
         */
        @JvmStatic
        fun calculateFrameSimilarity(frame1: MotionFrame, frame2: MotionFrame, width: Int, height: Int): Double {
            val leftArmSimilarity = HandNormalization.calculateArmSimilarity(frame1, frame2, "Left", width, height)
            val rightArmSimilarity = HandNormalization.calculateArmSimilarity(frame1, frame2, "Right", width, height)
            val handSimilarity = HandNormalization.calculateHandSimilarity(frame1, frame2, width, height)

            // 자바 서버 코드와 동일: 세 유사도 중 최댓값 사용
            return max(max(leftArmSimilarity, rightArmSimilarity), handSimilarity)
        }
    }
}

// 필요한 데이터 클래스
data class MotionFrame(
    val frame: Int,
    val poses: List<Pose>
)
