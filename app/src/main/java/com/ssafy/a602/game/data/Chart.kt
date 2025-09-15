package com.ssafy.a602.game.data

/**
 * ERD의 chart 테이블과 매핑되는 데이터 클래스
 * 리듬게임의 채보(차트) 정보를 담음
 */
data class Chart(
    val id: Long,                    // ERD: chart.id (BIGINT)
    val musicId: Long,               // ERD: chart.music_id (BIGINT) - Song.id와 매핑
    val sequence: Int,               // ERD: chart.sequence (INT) - 채보 내 순서
    val lyrics: String,              // ERD: chart.lyrics (VARCHAR(50)) - 가사
    val startedAt: String,           // ERD: chart.started_at (TIME) - 시작 시간
    val answers: List<ChartAnswer> = emptyList() // ERD: chart_answer 테이블과 매핑
)

/**
 * ERD의 chart_answer 테이블과 매핑되는 데이터 클래스
 * 채보의 정답 정보를 담음
 */
data class ChartAnswer(
    val id: Long,                    // ERD: chart_answer.id (BIGINT)
    val chartId: Long,               // ERD: chart_answer.chart_id (BIGINT)
    val startedAt: String,           // ERD: chart_answer.started_at (TIME)
    val endedAt: String,             // ERD: chart_answer.ended_at (TIME)
    val startedIndex: Int,           // ERD: chart_answer.started_index (TINYINT)
    val endedIndex: Int              // ERD: chart_answer.ended_index (TINYINT)
)
