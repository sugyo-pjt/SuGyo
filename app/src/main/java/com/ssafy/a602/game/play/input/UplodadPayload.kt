package com.ssafy.a602.game.play.input

@kotlinx.serialization.Serializable
data class UploadPayload(
    val segment: Int,
    val frames: List<FramePack.UploadFrame>
)
