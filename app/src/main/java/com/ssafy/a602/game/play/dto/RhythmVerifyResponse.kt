package com.ssafy.a602.game.play.dto

import kotlinx.serialization.Serializable

/**
 * 리듬 검증 응답 DTO
 */
@kotlinx.serialization.Serializable
data class RhythmVerifyResponse(
    val serverScore: Int,
    val accuracy: Float,
    val rank: String,
    val rejudged: Boolean,
    val diff: ScoreDiff? = null
)

/**
 * 점수 차이 정보
 */
@kotlinx.serialization.Serializable
data class ScoreDiff(
    val clientScore: Int,
    val serverScore: Int,
    val difference: Int
)
