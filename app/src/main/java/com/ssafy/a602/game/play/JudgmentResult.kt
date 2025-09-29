package com.ssafy.a602.game.play

/**
 * 게임 판정 결과를 나타내는 데이터 클래스
 */
data class JudgmentResult(
    val type: JudgmentType,
    val accuracy: Float,        // 0.0 ~ 1.0
    val score: Int,            // 획득 점수
    val combo: Int,            // 현재 콤보
    val timestamp: Long,        // 판정 시간
    // 로컬 판정을 위한 추가 필드
    val word: String? = null,   // 판정된 단어
    val isLocalResult: Boolean = true  // 로컬에서 계산된 판정인지 구분
)

/**
 * 판정 타입
 */
enum class JudgmentType {
    PERFECT,    // 완벽
    GREAT,      // 좋음
    GOOD,       // 보통
    MISS        // 실패
}

// 로컬 판정 결과를 위한 확장 함수
fun JudgmentResult.fromLocal(
    judgment: String,
    word: String,
    score: Int,
    combo: Int,
    totalScore: Int? = null,
    maxCombo: Int? = null,
    accuracy: Float? = null,
    grade: String? = null
): JudgmentResult {
    val type = when (judgment) {
        "PERFECT" -> JudgmentType.PERFECT
        "GREAT" -> JudgmentType.GREAT
        "GOOD" -> JudgmentType.GOOD
        "MISS" -> JudgmentType.MISS
        else -> JudgmentType.MISS
    }
    
    return JudgmentResult(
        type = type,
        accuracy = accuracy ?: when (type) {
            JudgmentType.PERFECT -> 0.98f
            JudgmentType.GREAT -> 0.85f
            JudgmentType.GOOD -> 0.70f
            JudgmentType.MISS -> 0.0f
        },
        score = score,
        combo = combo,
        timestamp = System.currentTimeMillis(),
        word = word,
        isLocalResult = true
    )
}
