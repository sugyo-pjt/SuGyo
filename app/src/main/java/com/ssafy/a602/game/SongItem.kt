package com.ssafy.a602.game


data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val durationText: String, // "3:14"
    val bpm: Int,
    val rating: Double,       // 0.0~5.0
    val bestScore: Int?,      // null = 기록 없음
    val thumbnailRes: Int? = null // drawable 리소스 ID
)