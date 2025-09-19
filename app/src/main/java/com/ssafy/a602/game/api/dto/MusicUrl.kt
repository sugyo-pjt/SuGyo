package com.ssafy.a602.game.api.dto

import com.squareup.moshi.JsonClass

/**
 * 노래(음원) 다운로드 URL API 응답 DTO
 * GET /api/v1/game/rhythm/music/{music_id}
 */
@JsonClass(generateAdapter = true)
data class MusicUrl(
    val musicUrl: String?
)
