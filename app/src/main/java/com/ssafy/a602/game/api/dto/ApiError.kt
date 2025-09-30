package com.ssafy.a602.game.api.dto

import com.google.gson.annotations.SerializedName

/**
 * API 에러 응답 DTO
 * 공통 에러 포맷에 맞춰 설계
 */
data class ApiError(
    @SerializedName("status")
    val status: Int,
    
    @SerializedName("code")
    val code: String,
    
    @SerializedName("message")
    val message: String
)
