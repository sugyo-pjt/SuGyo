package com.ssafy.a602.game.api.dto

import com.squareup.moshi.JsonClass

/**
 * 차트 섹션(소절) DTO - Swagger 1:1 매핑
 * 가사 및 채보 다운로드 API 응답의 배열 요소
 * GET /api/v1/game/rhythm/music/{music_id}/chart
 */
@JsonClass(generateAdapter = true)
data class ChartSegmentDto(
    val segment: Int,
    val barStartedAt: String,    // "HH:MM:SS" or "HH:MM:SS.SSS"
    val barEndedAt: String?,     // 스웨거엔 있음. 백엔드에서 null일 수도 있으니 nullable
    val lyrics: String,
    val correct: List<CorrectDto>
)

@JsonClass(generateAdapter = true)
data class CorrectDto(
    val correctStartedIndex: Int,
    val correctEndedIndex: Int,
    val actionStartedAt: String,
    val actionEndedAt: String
)
