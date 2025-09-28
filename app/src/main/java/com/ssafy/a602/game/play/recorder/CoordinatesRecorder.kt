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
        this.startedAtMs = 0L // 더 이상 사용하지 않음
        segments.clear()
        currentSegmentStart = -1L
        Log.d(TAG, "세션 시작: musicId=$musicId, startedAtMs=$startedAtMs")
    }

    fun appendFrame(nowMsAbs: Long, body: List<Vec4?>?, left: List<Vec4?>?, right: List<Vec4?>?) {
        val rel = nowMsAbs.coerceAtLeast(0L) // 이미 음악 재생 시간이므로 그대로 사용
        val segStart = (rel / segmentMs) * segmentMs   // 0, 300, 600, 900, 1200...
        
        // 첫 번째 세그먼트는 항상 0부터 시작하도록 강제
        val actualSegStart = if (segments.isEmpty()) 0L else segStart

        val seg = if (segments.isEmpty() || currentSegmentStart != actualSegStart) {
            currentSegmentStart = actualSegStart
            MutablePlaySegment(
                type = "PLAY",
                timestamp = actualSegStart,
                frames = mutableListOf()
            ).also { 
                segments += it
                Log.d(TAG, "새 세그먼트 생성: timestamp=$actualSegStart")
            }
        } else segments.last()

        val frameIndexInSegment = ((rel - actualSegStart) / frameIntervalMs).toInt().coerceAtLeast(0)
        val poses = buildList {
            body?.let { add(PoseBlock("BODY", it)) }
            left?.let { add(PoseBlock("LEFT_HAND", it)) }
            right?.let { add(PoseBlock("RIGHT_HAND", it)) }
        }
        
        if (poses.isNotEmpty()) {
            seg.frames += FrameBlock(frame = frameIndexInSegment, poses = poses)
            Log.v(TAG, "프레임 추가: segment=${actualSegStart}ms, frame=$frameIndexInSegment, poses=${poses.size}")
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
    
    // 🎯 판정용 최신 프레임 데이터 접근자
    fun getLatestFrameForJudgment(): List<FrameBlock>? {
        if (segments.isEmpty()) return null
        val latestSegment = segments.last()
        if (latestSegment.frames.isEmpty()) return null
        return latestSegment.frames
    }

    private data class MutablePlaySegment(
        val type: String,
        val timestamp: Long,
        val frames: MutableList<FrameBlock>
    )
}
