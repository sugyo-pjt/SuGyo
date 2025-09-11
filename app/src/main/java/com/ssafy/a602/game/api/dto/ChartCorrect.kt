package com.ssafy.a602.game.api.dto

import com.google.gson.annotations.SerializedName

/**
 * 차트 정답 정보 DTO
 * 가사 및 채보 다운로드 API 응답의 correct 배열 요소
 */
data class ChartCorrect(
    @SerializedName("correctStartedIndex")
    val correctStartedIndex: Int,
    
    @SerializedName("correctEndedIndex")
    val correctEndedIndex: Int,
    
    @SerializedName("actionStartedAt")
    val actionStartedAt: String, // "HH:MM:SS.xx" 형식
    
    @SerializedName("actionEndedAt")
    val actionEndedAt: String // "HH:MM:SS.xx" 형식
)
