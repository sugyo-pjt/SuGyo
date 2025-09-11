package com.ssafy.a602.game.api.dto

import com.google.gson.annotations.SerializedName

/**
 * 노래 목록 조회 API 응답 DTO
 * GET /api/v1/game/rhythm/music/list
 */
data class MusicListItem(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("singer")
    val singer: String,
    
    @SerializedName("songTime")
    val songTime: String, // "HH:MM:SS" 형식
    
    @SerializedName("albumImageUrl")
    val albumImageUrl: String,
    
    @SerializedName("myScore")
    val myScore: Long
)
