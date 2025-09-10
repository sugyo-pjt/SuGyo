package com.ssafy.a602.game


data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val durationText: String, // "3:14" - ERD의 song_time과 매핑
    val bpm: Int,
    val rating: Double,       // 0.0~5.0
    val bestScore: Int?,      // null = 기록 없음 - ERD의 rank 테이블과 매핑
    val thumbnailRes: Int? = null, // drawable 리소스 ID
    val audioUrl: String? = null,   // 음악 파일 URL
    val albumImageUrl: String? = null // ERD의 album_image_url과 매핑
)