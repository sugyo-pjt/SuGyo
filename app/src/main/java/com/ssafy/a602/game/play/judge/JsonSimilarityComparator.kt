package com.ssafy.a602.game.play.judge

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
        fun calculateMotionSimilarity(againstChart: List<MotionFrame>, correctChart: List<MotionFrame>, width: Int, height: Int): Float {
            val minFrames = minOf(againstChart.size, correctChart.size)
            if (minFrames == 0) {
                return 0f
            }

            var maxSimilarity = 0f
            for (i in againstChart.indices) {
                for (j in correctChart.indices) {
                    val frameSimilarity = calculateFrameSimilarity(againstChart[i], correctChart[i], width, height)
                    maxSimilarity = maxOf(frameSimilarity, maxSimilarity)
                }
            }

            return maxSimilarity
        }

        /**
         * 각 프레임 별 유사도 계산
         */
        @JvmStatic
        fun calculateFrameSimilarity(frame1: MotionFrame, frame2: MotionFrame, width: Int, height: Int): Float {
            // 각 신체 부위별 유사도 계산 (왼팔,오른팔,손(왼손,오른손))
            val leftArmSimilarity = calculateArmSimilarity(frame1, frame2, "Left", width, height)
            val rightArmSimilarity = calculateArmSimilarity(frame1, frame2, "Right", width, height)
            val handSimilarity = calculateHandSimilarity(frame1, frame2, width, height)

            // 가중 평균: 팔(왼40% + 오른40%) + 손(20%)
            val totalSimilarity = (leftArmSimilarity * 0.2f + rightArmSimilarity * 0.2f + handSimilarity * 0.6f)

            return totalSimilarity
        }

        /**
         * 한 프레임에 대한 팔(관절) 유사도 계산
         */
        @JvmStatic
        fun calculateArmSimilarity(frame1: MotionFrame, frame2: MotionFrame, side: String, width: Int, height: Int): Float {
            val poses1 = frame1.poses
            val poses2 = frame2.poses

            val armFeature1 = HandNormalization.armFeatureVector(poses1, width, height, side)
            val armFeature2 = HandNormalization.armFeatureVector(poses2, width, height, side)

            if (armFeature1 == null || armFeature2 == null) {
                return 0f
            }

            return HandNormalization.cosineSimilarity(armFeature1, armFeature2)
        }

        /**
         * MotionFrame에서 특정 신체 부위의 좌표를 추출하는 헬퍼 메서드
         */
        private fun getHandCoordinates(frame: MotionFrame, bodyPart: BodyPart): List<Coordinate> {
            for (pose in frame.poses) {
                if (pose.part == bodyPart) {
                    return pose.coordinates
                }
            }
            return emptyList()
        }

        /**
         * 한 프레임에 대한 손(왼손,오른손) 유사도 계산
         */
        @JvmStatic
        fun calculateHandSimilarity(frame1: MotionFrame, frame2: MotionFrame, width: Int, height: Int): Float {
            // 각 프레임에서 손 좌표 추출
            val leftHand1 = getHandCoordinates(frame1, BodyPart.LEFT_HAND)
            val rightHand1 = getHandCoordinates(frame1, BodyPart.RIGHT_HAND)
            val leftHand2 = getHandCoordinates(frame2, BodyPart.LEFT_HAND)
            val rightHand2 = getHandCoordinates(frame2, BodyPart.RIGHT_HAND)

            // 각 손의 유사도 계산
            val leftSimilarity = calculateSingleHandSimilarity(leftHand1, leftHand2, width, height)
            val rightSimilarity = calculateSingleHandSimilarity(rightHand1, rightHand2, width, height)

            return (leftSimilarity + rightSimilarity) / 2f
        }

        /**
         * 한 손에 대한 유사도 계산
         */
        private fun calculateSingleHandSimilarity(hand1: List<Coordinate>, hand2: List<Coordinate>, width: Int, height: Int): Float {
            if (hand1.isEmpty() || hand2.isEmpty()) {
                return 0f
            }

            if (hand1.size != hand2.size) {
                return 0f
            }

            val handFeature1 = HandNormalization.handFeatureVector(hand1, width, height)
            val handFeature2 = HandNormalization.handFeatureVector(hand2, width, height)

            return HandNormalization.cosineSimilarity(handFeature1, handFeature2)
        }
    }
}

// 필요한 데이터 클래스
data class MotionFrame(
    val frame: Int,
    val poses: List<Pose>
)
