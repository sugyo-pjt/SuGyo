package com.ssafy.a602.game.api.dto

import com.squareup.moshi.JsonClass

/**
 * 노래 목록 조회 API 응답 DTO
 * GET /api/v1/game/rhythm/music/list
 */
@JsonClass(generateAdapter = true)
data class MusicListItem(
    val id: Long,
    val title: String,
    val singer: String,
    val songTime: String, // "HH:MM:SS" 형식
    val albumImageUrl: String,
    val myScore: Long? // null 허용
)
