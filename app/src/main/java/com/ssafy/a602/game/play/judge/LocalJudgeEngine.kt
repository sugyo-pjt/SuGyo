package com.ssafy.a602.game.play.judge

class LocalJudgeEngine {
    
    companion object {
        private const val SIMILARITY_THRESHOLD = 0.6f
        private const val PERFECT_RATIO_THRESHOLD = 0.9f  // 자바 서버와 동일
        private const val GOOD_RATIO_THRESHOLD = 0.7f     // 자바 서버와 동일
    }

    /**
     * 유사도 → 등급 판정
     */
    fun judgeByRatio(ratio: Float): Judgment {
        android.util.Log.d("LocalJudgeEngine", "⚖️ 판정 로직 실행:")
        android.util.Log.d("LocalJudgeEngine", "  - 입력 유사도: $ratio")
        android.util.Log.d("LocalJudgeEngine", "  - PERFECT 임계값: $PERFECT_RATIO_THRESHOLD")
        android.util.Log.d("LocalJudgeEngine", "  - GOOD 임계값: $GOOD_RATIO_THRESHOLD")
        
        val judgment = when {
            ratio >= PERFECT_RATIO_THRESHOLD -> {
                android.util.Log.d("LocalJudgeEngine", "  - 판정: PERFECT (${ratio} >= ${PERFECT_RATIO_THRESHOLD})")
                Judgment.PERFECT
            }
            ratio >= GOOD_RATIO_THRESHOLD -> {
                android.util.Log.d("LocalJudgeEngine", "  - 판정: GOOD (${ratio} >= ${GOOD_RATIO_THRESHOLD})")
                Judgment.GOOD
            }
            else -> {
                android.util.Log.d("LocalJudgeEngine", "  - 판정: MISS (${ratio} < ${GOOD_RATIO_THRESHOLD})")
                Judgment.MISS
            }
        }
        
        return judgment
    }

    /**
     * 등급 + 콤보 → 점수 계산 - 자바 서버 코드와 일치
     */
    fun calculatePoints(currentJudge: Judgment, context: GameSessionContext): Int {
        val combo = context.combo.get()
        val points = when (currentJudge) {
            Judgment.PERFECT -> {
                val basePoints = 100
                val comboBonus = combo / 10
                val totalPoints = basePoints + comboBonus
                android.util.Log.d("LocalJudgeEngine", "💰 PERFECT 점수 계산: 기본($basePoints) + 콤보보너스($comboBonus) = $totalPoints")
                totalPoints
            }
            Judgment.GOOD -> {
                val basePoints = 70
                val comboBonus = combo / 20
                val totalPoints = basePoints + comboBonus
                android.util.Log.d("LocalJudgeEngine", "💰 GOOD 점수 계산: 기본($basePoints) + 콤보보너스($comboBonus) = $totalPoints")
                totalPoints
            }
            Judgment.MISS -> {
                android.util.Log.d("LocalJudgeEngine", "💰 MISS 점수 계산: 0")
                0
            }
        }
        return points
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
