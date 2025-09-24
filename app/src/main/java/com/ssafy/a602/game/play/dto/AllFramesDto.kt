package com.ssafy.a602.game.play.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class GameActionType { 
    PLAY, 
    PAUSE, 
    RESUME 
}

@Serializable
enum class PosePart { 
    BODY, 
    LEFT_HAND, 
    RIGHT_HAND 
}

@Serializable
data class Coordinate(
    val x: Float, 
    val y: Float, 
    val z: Float, 
    val w: Float
)

@Serializable
data class PoseBlock(
    val part: String,              // "BODY", "LEFT_HAND", "RIGHT_HAND" (명세에 맞게 String으로 변경)
    val coordinates: List<Coordinate>
)

@Serializable
data class FrameBlock(
    val frame: Int,                // 1..N (윈도우 내 1-base)
    val poses: List<PoseBlock>     // BODY, LEFT_HAND, RIGHT_HAND
)

// 통합된 요청 DTO (HTTP 명세에 맞게)
@Serializable
data class SimilarityRequest(
    val type: String,              // "PLAY", "PAUSE", "RESUME"
    val timestamp: Long,           // 0, 300, 600, 900 ... 노래 처음부터 지난 시점 millisecond
    val frames: List<FrameBlock>
)

// 통합된 응답 DTO (HTTP 명세에 맞게)
@Serializable
data class SimilarityResponse(
    val similarity: Float,
    val timestamp: Long,
    val musicId: Long
)

// 기존 웹소켓용 래퍼 (하위 호환성 유지)
@Serializable
data class ActionFrames(
    @SerialName("GameActionType") val action: GameActionType,
    val timestamp: Long,           // 플레이어 위치(ms)
    val frames: List<FrameBlock>   // PAUSE/RESUME일 땐 빈 리스트 가능
)

@Serializable
data class AllFramesEnvelope(
    @SerialName("AllFrames") val allFrames: List<ActionFrames>
)

// 웹소켓에서 받는 판정 결과 (기존 구조 유지)
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
