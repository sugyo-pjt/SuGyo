package com.ssafy.a602.game.play.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 웹소켓 통신용 DTO들
 */

// 웹소켓에서 받는 판정 결과
@Serializable
data class WebSocketJudgmentResult(
    val judgment: String,           // "PERFECT", "GREAT", "GOOD", "MISS"
    val word: String,               // 판정된 단어
    val timestamp: Long,            // 판정 시점
    val score: Int,                 // 획득 점수
    val combo: Int,                 // 현재 콤보
    // 서버에서 계산된 추가 정보
    val totalScore: Int? = null,    // 누적 총 점수
    val maxCombo: Int? = null,      // 최대 콤보
    val accuracy: Float? = null,   // 정확도 (0.0~1.0)
    val grade: String? = null      // 등급 (S, A, B, C, F)
)

// HTTP 유사도 요청용 DTO
@Serializable
data class SimilarityRequest(
    val type: String,              // "PLAY", "PAUSE", "RESUME"
    val timestamp: Long,           // 0, 300, 600, 900 ... 노래 처음부터 지난 시점 millisecond
    val frames: List<FrameBlock>
)

// HTTP 유사도 응답용 DTO
@Serializable
data class SimilarityResponse(
    val similarity: Float,
    val timestamp: Long,
    val musicId: Long
)

// 프레임 블록 (HTTP 요청용)
@Serializable
data class FrameBlock(
    val frame: Int,                // 1..N (윈도우 내 1-base)
    val poses: List<PoseBlock>     // BODY, LEFT_HAND, RIGHT_HAND
)

// 포즈 블록 (HTTP 요청용)
@Serializable
data class PoseBlock(
    val part: String,              // "BODY", "LEFT_HAND", "RIGHT_HAND"
    val coordinates: List<Coordinate>
)

// 좌표 (HTTP 요청용)
@Serializable
data class Coordinate(
    val x: Float, 
    val y: Float, 
    val z: Float, 
    val w: Float
)
