package com.ssafy.a602.game

/**
 * 순위 아이템 데이터 클래스
 */
data class RankingItem(
    val rank: Int,          // 1, 2, 3, ...
    val nickname: String,   // 사용자 닉네임
    val score: Int,         // 점수
    val avatarUrl: String? = null, // 아바타 이미지 URL (선택사항)
    val userId: String? = null     // 사용자 ID (선택사항)
) {
    /**
     * 점수를 포맷된 문자열로 변환 (예: 987,650)
     */
    val formattedScore: String
        get() = String.format("%,d", score)
}
