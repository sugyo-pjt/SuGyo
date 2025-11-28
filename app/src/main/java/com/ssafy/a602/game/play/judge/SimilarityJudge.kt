package com.ssafy.a602.game.play.judge

import android.util.Log
import com.ssafy.a602.game.play.answer.AnswerFrame
import kotlin.math.sqrt

/**
 * 유사도 계산 및 판정 클래스
 */
class SimilarityJudge {
    private val TAG = "SimilarityJudge"
    
    // 등급 임계값 - 자바 서버와 일치
    companion object {
        const val PERFECT_THRESHOLD = 0.9f   // 자바 서버: 0.9
        const val GOOD_THRESHOLD = 0.7f      // 자바 서버: 0.7
    }
    
    // 가중치
    private val poseWeight = 0.5f
    private val leftWeight = 0.25f
    private val rightWeight = 0.25f
    
    /**
     * 사용자 프레임과 정답 프레임의 유사도를 계산하여 등급 반환
     * @param userFrame 사용자 프레임
     * @param answerFrame 정답 프레임
     * @return Pair<등급, 유사도>
     */
    fun calculateGrade(userFrame: FrameFeature, answerFrame: AnswerFrame): Pair<String, Float> {
        return try {
            val similarity = calculateSimilarity(userFrame, answerFrame)
            val grade = determineGrade(similarity)
            
            Log.d(TAG, "판정 결과: grade=$grade, similarity=$similarity")
            Pair(grade, similarity)
        } catch (e: Exception) {
            Log.e(TAG, "유사도 계산 실패", e)
            Pair("MISS", 0f)
        }
    }
    
    /**
     * 가중 코사인 유사도 계산
     * sim = 0.5*cos(pose) + 0.25*cos(left) + 0.25*cos(right)
     */
    private fun calculateSimilarity(userFrame: FrameFeature, answerFrame: AnswerFrame): Float {
        val poseSim = calculateCosineSimilarity(userFrame.pose, answerFrame.pose)
        val leftSim = calculateCosineSimilarity(userFrame.left, answerFrame.left)
        val rightSim = calculateCosineSimilarity(userFrame.right, answerFrame.right)
        
        val weightedSim = poseSim * poseWeight + leftSim * leftWeight + rightSim * rightWeight
        
        Log.v(TAG, "유사도 계산: pose=$poseSim, left=$leftSim, right=$rightSim, weighted=$weightedSim")
        
        return weightedSim.coerceIn(0f, 1f)
    }
    
    /**
     * 코사인 유사도 계산
     */
    private fun calculateCosineSimilarity(vec1: FloatArray, vec2: FloatArray): Float {
        if (vec1.size != vec2.size) {
            Log.w(TAG, "벡터 크기 불일치: ${vec1.size} vs ${vec2.size}")
            return 0f
        }
        
        val dotProduct = vec1.zip(vec2) { a, b -> a * b }.sum()
        val magnitude1 = sqrt(vec1.sumOf { (it * it).toDouble() }).toFloat()
        val magnitude2 = sqrt(vec2.sumOf { (it * it).toDouble() }).toFloat()
        
        if (magnitude1 == 0f || magnitude2 == 0f) {
            return 0f
        }
        
        return (dotProduct / (magnitude1 * magnitude2)).coerceIn(-1f, 1f)
    }
    
    /**
     * 유사도에 따른 등급 결정
     */
    private fun determineGrade(similarity: Float): String {
        return when {
            similarity >= PERFECT_THRESHOLD -> "PERFECT"
            similarity >= GOOD_THRESHOLD -> "GOOD"
            else -> "MISS"
        }
    }
    
    /**
     * 등급에 따른 점수 계산
     */
    fun calculateScore(grade: String, baseScore: Int = 100): Int {
        return when (grade) {
            "PERFECT" -> baseScore
            "GOOD" -> (baseScore * 0.7f).toInt()
            "MISS" -> 0
            else -> 0
        }
    }
    
    /**
     * 등급에 따른 콤보 증가 여부
     */
    fun shouldIncreaseCombo(grade: String): Boolean {
        return grade in listOf("PERFECT", "GOOD")
    }
    
    /**
     * 등급에 따른 게이지 증가량
     */
    fun getGaugeIncrease(grade: String): Float {
        return when (grade) {
            "PERFECT" -> 0.1f
            "GOOD" -> 0.05f
            "MISS" -> -0.05f
            else -> 0f
        }
    }
}
