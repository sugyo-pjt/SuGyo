package com.ssafy.a602.game.songs

data class SongItem(
    val id: String,
    val title: String,
    val artist: String,
    val durationText: String, // "3:14" - API의 songTime과 매핑
    val bestScore: Int?,      // null = 기록 없음 - API의 myScore와 매핑
    val albumImageUrl: String? = null // API의 albumImageUrl과 매핑
)