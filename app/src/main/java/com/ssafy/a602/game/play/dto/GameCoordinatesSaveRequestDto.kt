package com.ssafy.a602.game.play.dto

import kotlinx.serialization.Serializable

@kotlinx.serialization.Serializable
data class GameCoordinatesSaveRequestDto(
    val musicId: Long,
    val allFrames: List<PlaySegment>
)
