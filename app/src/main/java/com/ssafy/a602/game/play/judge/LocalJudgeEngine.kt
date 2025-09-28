package com.ssafy.a602.game.play.judge

class LocalJudgeEngine {
    
    companion object {
        private const val SIMILARITY_THRESHOLD = 0.6f
        private const val PERFECT_RATIO_THRESHOLD = 0.9f
        private const val GOOD_RATIO_THRESHOLD = 0.7f
    }

    /**
     * 유사도 → 등급 판정
     */
    fun judgeByRatio(ratio: Float): Judgment {
        return when {
            ratio >= PERFECT_RATIO_THRESHOLD -> Judgment.PERFECT
            ratio >= GOOD_RATIO_THRESHOLD -> Judgment.GOOD
            else -> Judgment.MISS
        }
    }

    /**
     * 등급 + 콤보 → 점수 계산
     */
    fun calculatePoints(currentJudge: Judgment, context: GameSessionContext): Int {
        return when (currentJudge) {
            Judgment.PERFECT -> 100 + (context.combo.get() / 10)
            Judgment.GOOD -> 70 + (context.combo.get() / 20)
            Judgment.MISS -> 0
        }
    }

    /**
     * 프레임 유사도 계산
     */
    fun calculateFrameSimilarity(userFrames: List<MotionFrame>, answerFrames: List<MotionFrame>): Float {
        return JsonSimilarityComparator.calculateMotionSimilarity(
            userFrames, answerFrames, 640, 480
        ).toFloat()
    }

    /**
     * 게임 판정 처리
     */
    fun processJudgment(
        context: GameSessionContext,
        userFrames: List<MotionFrame>,
        answerFrames: List<MotionFrame>
    ): Judgment {
        if (!context.isPlaying()) {
            return Judgment.MISS
        }

        val similarity = calculateFrameSimilarity(userFrames, answerFrames)
        val judgment = judgeByRatio(similarity)
        val points = calculatePoints(judgment, context)
        
        context.applyJudgment(points, judgment)
        
        return judgment
    }
}
