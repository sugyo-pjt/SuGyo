package com.ssafy.a602.game.api.dto

import com.google.gson.annotations.SerializedName

/**
 * 게임 완료 응답 DTO
 * Response: { "musicId": 1, "IsbestRecord": true }
 */
data class CompleteResp(
    val musicId: Long,
    @SerializedName("IsbestRecord") val isBestRecord: Boolean
)
