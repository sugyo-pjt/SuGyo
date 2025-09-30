package com.ssafy.a602.game.play.dto

import kotlinx.serialization.Serializable

@kotlinx.serialization.Serializable
data class FrameBlock(
    val frame: Int,                           // 세그먼트 내 프레임 index (0..)
    val poses: List<PoseBlock>
)
