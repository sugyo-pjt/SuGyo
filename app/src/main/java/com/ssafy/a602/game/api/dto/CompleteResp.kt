package com.ssafy.a602.game.api.dto

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Json

/**
 * 게임 완료 응답 DTO
 * Response: { "musicId": 1, "isBestRecord": true }
 */
@JsonClass(generateAdapter = true)
data class CompleteResp(
    val musicId: Long,
    @Json(name = "isBestRecord") val isBestRecord: Boolean
)
