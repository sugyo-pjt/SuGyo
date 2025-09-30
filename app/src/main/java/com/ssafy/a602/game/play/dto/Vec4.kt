package com.ssafy.a602.game.play.dto

import kotlinx.serialization.Serializable

@kotlinx.serialization.Serializable
data class Vec4(
    val x: Float?,
    val y: Float?,
    val z: Float?,
    val w: Float?
)
