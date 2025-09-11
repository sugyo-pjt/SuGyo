package com.ssafy.a602.game.play.input

@kotlinx.serialization.Serializable
data class FramePack(
    val tsMs: Long,
    val pose: List<LM>,   // pose 0..22 (23)
    val left: List<LM>,   // left hand 0..20 (21)
    val right: List<LM>   // right hand 0..20 (21)
)
