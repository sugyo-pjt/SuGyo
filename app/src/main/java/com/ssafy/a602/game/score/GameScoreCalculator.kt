package com.ssafy.a602.game.score

enum class JudgmentType { PERFECT, MISS }

data class GameResultRequest(
    val songId: String,
    val totalScore: Int,
    val correctCount: Int,
    val missCount: Int,
    val maxCombo: Int,
    val missWords: List<String>,
    val grade: String,
    val percent: Int
)

class GameScoreCalculator(
    private val songId: String,
    private val totalWords: Int,
    private val baseScore: Int = 100
) {
    private var currentCombo = 0
    private var totalScore = 0
    private var correctCount = 0
    private var missCount = 0
    private var maxCombo = 0
    private val missWords = mutableListOf<String>()

    // 콤보 1→1.0, 2→1.1, 3→1.2, 4→1.3, 5→1.4, 6+→1.5
    private fun comboMultiplier(c: Int): Double = minOf(1.5, 1.0 + (c - 1) * 0.1)

    fun addJudgment(type: JudgmentType, word: String) {
        when (type) {
            JudgmentType.PERFECT -> {
                currentCombo++
                val delta = (baseScore * comboMultiplier(currentCombo)).toInt() // 내림 일관
                totalScore += delta
                correctCount++
                if (currentCombo > maxCombo) maxCombo = currentCombo
            }
            JudgmentType.MISS -> {
                currentCombo = 0
                missCount++
                missWords.add(word)
            }
        }
    }

    // 풀콤(전부 PERFECT) 가정으로 최대점 계산 — totalWords 기준!
    private fun computeMaxScore(): Int {
        var s = 0
        var combo = 0
        repeat(totalWords) {
            combo++
            s += (baseScore * comboMultiplier(combo)).toInt()
        }
        return maxOf(1, s) // 분모 0 방지
    }

    // 퍼센트 기준 등급 (S=100%만)
    private fun gradeOfPercent(percent: Int): String = when {
        percent == 100 -> "S"
        percent >= 90 -> "A"
        percent >= 70 -> "B"
        percent >= 50 -> "C"
        else -> "F"
    }

    fun getFinal(): GameResultRequest {
        val maxScore = computeMaxScore()
        val percent = (totalScore * 100 / maxScore) // 정수 내림
        return GameResultRequest(
            songId = songId,
            totalScore = totalScore,
            correctCount = correctCount,
            missCount = missCount,
            maxCombo = maxCombo,
            missWords = missWords.toList(),
            grade = gradeOfPercent(percent),
            percent = percent
        )
    }

    fun reset() {
        currentCombo = 0
        totalScore = 0
        correctCount = 0
        missCount = 0
        maxCombo = 0
        missWords.clear()
    }
}
