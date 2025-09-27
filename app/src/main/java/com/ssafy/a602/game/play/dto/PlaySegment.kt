package com.ssafy.a602.game.play.dto

import kotlinx.serialization.Serializable

@kotlinx.serialization.Serializable
data class PlaySegment(
    val type: String,                         // "PLAY" (필요시 PAUSE/RESUME)
    val timestamp: Long,                      // 0, 300, 600 ... (노래 시작 기준 ms)
    val frames: List<FrameBlock>
)
