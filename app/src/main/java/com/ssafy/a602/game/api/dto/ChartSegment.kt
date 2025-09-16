package com.ssafy.a602.game.api.dto

import com.google.gson.annotations.SerializedName

/**
 * 차트 섹션(소절) DTO
 * 가사 및 채보 다운로드 API 응답의 배열 요소
 * GET /api/v1/game/rhythm/music/{music_id}/chart
 */
data class ChartSegment(
    @SerializedName("segment")
    val segment: Int,
    
    @SerializedName("barStartedAt")
    val barStartedAt: String, // "HH:MM:SS.xx" 형식
    
    @SerializedName("barEndedAt")
    val barEndedAt: String, // "HH:MM:SS.xx" 형식
    
    @SerializedName("lyrics")
    val lyrics: String,
    
    @SerializedName("correct")
    val correct: List<ChartCorrect>
)
