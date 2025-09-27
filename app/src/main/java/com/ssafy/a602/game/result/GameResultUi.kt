package com.ssafy.a602.game.result

/**
 * 게임 결과 화면에서 사용하는 UI 모델
 */
data class GameResultUi(
    val songTitle: String,
    val score: Int,
    val accuracyPercent: Int,   // 0~100
    val grade: String,          // "S" | "A" | "B" | "C" | "F" (서버 계산값 권장)
    val maxCombo: Int,
    val correctCount: Int,
    val missCount: Int,
    val comboMultiplier: Double, // 1.0 ~ 1.5
    val isNewRecord: Boolean,
    val missWords: List<String>,
    // API 응답용 추가 필드들
    val accepted: Boolean = true,
    val isPersonalBest: Boolean = false,
    val rankUpdated: Boolean = false,
    val serverScoreEcho: Int = 0,
    // HARD 모드용 추가 필드들
    val perfectCount: Int = 0,
    val goodCount: Int = 0,
    val totalCount: Int = 0,
    val avgSimilarity: Float = 0f,
    val gameMode: String = "EASY"  // "EASY" | "HARD"
)
