package com.ssafy.a602.game.play.answer

/**
 * 정답 타임라인 데이터
 * @param musicId 음악 ID
 * @param startMs 시작 시간 (ms)
 * @param frames 정답 프레임 리스트
 * @param answerHash 정답 해시 (선택사항)
 * @param judgeVersion 판정 버전 (선택사항)
 * @param offsetMs 오프셋 (ms, 기본값 0)
 */
data class AnswerTimeline(
    val musicId: Long,
    val startMs: Long,
    val frames: List<AnswerFrame>,
    val answerHash: String? = null,
    val judgeVersion: String? = null,
    val offsetMs: Long = 0L
) {
    companion object {
        const val FPS = 30f
        const val FRAME_INTERVAL_MS = 1000f / FPS // ≈ 33.333ms
    }
    
    /**
     * 현재 시간에 해당하는 정답 프레임 인덱스를 반환
     * @param nowMs 현재 시간 (ms)
     * @return 프레임 인덱스
     */
    fun indexAt(nowMs: Long): Int {
        val rel = (nowMs - startMs - offsetMs).coerceAtLeast(0L)
        val idx = kotlin.math.floor(rel / FRAME_INTERVAL_MS).toInt()
        return idx.coerceIn(0, frames.lastIndex)
    }
    
    /**
     * 현재 시간에 해당하는 정답 프레임을 반환
     * @param nowMs 현재 시간 (ms)
     * @return 정답 프레임 또는 null
     */
    fun frameAt(nowMs: Long): AnswerFrame? {
        val index = indexAt(nowMs)
        return frames.getOrNull(index)
    }
}
