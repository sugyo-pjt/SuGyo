package com.ssafy.a602.game.play.dto

import kotlinx.serialization.Serializable

@kotlinx.serialization.Serializable
data class RhythmResultRequest(
    val clientCoordinates: List<GameCoordinatesSaveRequestDto>, // 서버 명세가 "배열"이므로 List로 전송
    val clientCalculateScore: Int
)
