package com.ssafy.a602.game

/**
 * 순위 화면의 UI 상태
 */
data class RankingUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val songTitle: String = "",
    val top3Rankings: List<RankingItem> = emptyList(),
    val allRankings: List<RankingItem> = emptyList(),
    val myRanking: RankingItem? = null // 내 순위 (없으면 null)
)
