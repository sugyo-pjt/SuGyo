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
    // 웹소켓 판정을 위한 추가 필드
    val word: String? = null,   // 판정된 단어 (웹소켓에서 받은 경우)
    val isWebSocketResult: Boolean = false  // 웹소켓에서 온 판정인지 구분
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

