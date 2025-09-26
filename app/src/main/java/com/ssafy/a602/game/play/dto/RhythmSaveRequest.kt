package com.ssafy.a602.game.play.dto

import kotlinx.serialization.Serializable

/**
 * 하드 모드 리듬게임 데이터 저장 요청
 * 곡 종료 시 한 번에 업로드하는 API용 데이터 모델
 */
@Serializable
data class RhythmSaveRequest(
    val musicId: Int,
    val allFrames: List<SegmentDto>
)

/**
 * 세그먼트 정보 (PLAY/PAUSE/RESUME 단위)
 */
@Serializable
data class SegmentDto(
    val type: String,     // "PLAY", "PAUSE", "RESUME"
    val timestamp: Long,  // 0, 300, 600, 900... (300ms 단위)
    val frames: List<FrameDto>
)

/**
 * 300ms 단위 프레임 묶음
 */
@Serializable
data class FrameBundle(
    val timestamp: Long,  // 0, 300, 600, 900... (300ms 단위)
    val frames: List<FrameDto>
)

/**
 * 프레임 정보 (300ms 주기)
 */
@Serializable
data class FrameDto(
    val frame: Int,  // 전역 프레임 인덱스
    val poses: List<PoseDto>
)

/**
 * 포즈 정보
 */
@Serializable
data class PoseDto(
    val part: String,     // "BODY", "LEFT_HAND", "RIGHT_HAND"
    val coordinates: List<CoordinateDto>
)

/**
 * 좌표 정보 (수어 인식되지 않을 때 null)
 */
@Serializable
data class CoordinateDto(
    val x: Float?,
    val y: Float?,
    val z: Float?,
    val w: Float?
)

