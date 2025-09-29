package com.ssafy.a602.game.play.dto

import kotlinx.serialization.Serializable

@kotlinx.serialization.Serializable
data class PoseBlock(
    val part: String,                         // "BODY" | "LEFT_HAND" | "RIGHT_HAND"
    val coordinates: List<Vec4?>
)
