package com.ssafy.a602.game.play.dto

import kotlinx.serialization.Serializable

/**
 * 리듬 검증 요청 DTO
 */
@kotlinx.serialization.Serializable
data class RhythmVerifyRequest(
    val musicId: Long,
    val answerHash: String? = null,
    val judgeVersion: String? = null,
    val startedAt: Long,
    val endedAt: Long,
    val summary: GameSummary,
    val samples: List<SampleData>,
    val featureSpecVersion: String? = null,
    val device: DeviceInfo? = null
)

/**
 * 게임 요약 정보
 */
@kotlinx.serialization.Serializable
data class GameSummary(
    val avgSim: Float,
    val perfectRate: Float,
    val goodRate: Float,
    val missRate: Float,
    val maxCombo: Int,
    val clientScore: Int
)

/**
 * 샘플 데이터 (검증용)
 */
@kotlinx.serialization.Serializable
data class SampleData(
    val timestamp: Long,
    val similarity: Float
)

/**
 * 디바이스 정보
 */
@kotlinx.serialization.Serializable
data class DeviceInfo(
    val model: String,
    val os: String
)
