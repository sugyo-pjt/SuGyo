package com.ssafy.a602.game.data

import com.ssafy.a602.game.api.dto.CorrectDto

/**
 * 곡의 소절 정보를 담는 데이터 클래스
 */
data class SongSection(
    val id: String,
    val songId: String,
    val startTime: Float,      // 시작 시간 (초)
    val endTime: Float,        // 종료 시간 (초)
    val text: String,          // 가사 텍스트
    val difficulty: Int = 1,   // 난이도 (1-5)
    val correctInfo: List<CorrectDto> = emptyList() // 수어 타이밍 및 하이라이팅 정보
)
