package com.ssafy.a602.game.data

/**
 * 게임 진행 상태를 담는 데이터 클래스
 */
data class SongProgress(
    val songId: String,
    val currentTime: Float,        // 현재 재생 시간 (초)
    val totalTime: Float,          // 총 재생 시간 (초)
    val currentSectionIndex: Int,  // 현재 소절 인덱스
    val sections: List<SongSection>, // 모든 소절 정보
    val score: Int = 0,            // 현재 점수
    val combo: Int = 0,            // 현재 콤보
    val accuracy: Float = 0f       // 정확도 (0.0-1.0)
)
