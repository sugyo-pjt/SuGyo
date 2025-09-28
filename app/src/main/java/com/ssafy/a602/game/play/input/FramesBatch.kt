package com.ssafy.a602.game.play.input

import kotlinx.serialization.Serializable

@Serializable
data class FrameEntry(
    val frameIndex: Int,            // 윈도우(0.3s) 내 0부터 시작
    val pose: List<LM?>,            // 0~22 총 23개 (null 허용)
    val left: List<LM?>,           // 0~20 총 21개 (null 허용)
    val right: List<LM?>            // 0~20 총 21개 (null 허용)
)

@Serializable
data class FramesBatch(
    val frames: List<FrameEntry>    // 오직 이 키 하나만 최상위에 존재
)
