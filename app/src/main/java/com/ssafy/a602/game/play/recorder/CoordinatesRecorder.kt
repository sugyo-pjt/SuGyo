package com.ssafy.a602.game.play.recorder

import com.ssafy.a602.game.play.dto.*
import android.util.Log

/**
 * 플레이 중 원본 좌표를 세그먼트/프레임 구조로 축적 → 종료 시 DTO로 변환
 */
class CoordinatesRecorder(
    private val musicId: Long,
    private val frameIntervalMs: Float = 1000f / 30f,  // fps=30
    private val segmentMs: Long = 300L
) {
    companion object {
        private const val TAG = "CoordinatesRecorder"
    }
    
    private var startedAtMs: Long = 0L  // 노래 시작 시각(absolute ms)
    private var currentSegmentStart: Long = -1L
    private val segments = mutableListOf<MutablePlaySegment>()

    fun startSession(startedAtMs: Long) { 
        this.startedAtMs = startedAtMs
        segments.clear()
        currentSegmentStart = -1L
        Log.d(TAG, "세션 시작: musicId=$musicId, startedAtMs=$startedAtMs")
    }

    fun appendFrame(nowMsAbs: Long, body: List<Vec4?>?, left: List<Vec4?>?, right: List<Vec4?>?) {
        val rel = (nowMsAbs - startedAtMs).coerceAtLeast(0L)
        val segStart = (rel / segmentMs) * segmentMs   // 0, 300, 600 ...

        val seg = if (segments.isEmpty() || currentSegmentStart != segStart) {
            currentSegmentStart = segStart
            MutablePlaySegment(
                type = "PLAY",
                timestamp = segStart,
                frames = mutableListOf()
            ).also { 
                segments += it
                Log.d(TAG, "새 세그먼트 생성: timestamp=$segStart")
            }
        } else segments.last()

        val frameIndexInSegment = ((rel - segStart) / frameIntervalMs).toInt().coerceAtLeast(0)
        val poses = buildList {
            body?.let { add(PoseBlock("BODY", it)) }
            left?.let { add(PoseBlock("LEFT_HAND", it)) }
            right?.let { add(PoseBlock("RIGHT_HAND", it)) }
        }
        
        if (poses.isNotEmpty()) {
            seg.frames += FrameBlock(frame = frameIndexInSegment, poses = poses)
            Log.v(TAG, "프레임 추가: segment=${segStart}ms, frame=$frameIndexInSegment, poses=${poses.size}")
        }
    }

    fun buildDto(): GameCoordinatesSaveRequestDto {
        val finalized = segments.map { PlaySegment(it.type, it.timestamp, it.frames) }
        Log.d(TAG, "DTO 빌드 완료: segments=${finalized.size}, totalFrames=${finalized.sumOf { it.frames.size }}")
        return GameCoordinatesSaveRequestDto(musicId = musicId, allFrames = finalized)
    }

    fun reset() { 
        segments.clear()
        currentSegmentStart = -1L
        startedAtMs = 0L
        Log.d(TAG, "리코더 리셋")
    }

    private data class MutablePlaySegment(
        val type: String,
        val timestamp: Long,
        val frames: MutableList<FrameBlock>
    )
}
