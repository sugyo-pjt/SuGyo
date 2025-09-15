package com.ssafy.a602.game.api.dto

/**
 * 게임 완료 요청 DTO
 * Request: { musicId: 1, score: 1150 }
 */
data class CompleteReq(
    val musicId: Long,
    val score: Int
)
