package com.ssafy.a602.game.play.input

import android.util.Log

/**
 * 동적 랜드마크 버퍼
 * actionStartedAt과 actionEndedAt 시간에 맞춰 동적으로 버퍼를 관리
 */
class DynamicLandmarkBuffer {
    private val buf = ArrayDeque<FramePack>()
    private val lock = Any()
    
    // 현재 활성화된 수어 타이밍 정보
    private var currentActionStart: Long? = null
    private var currentActionEnd: Long? = null
    private var currentWordId: String? = null

    /**
     * 새로운 프레임 추가
     * 현재 활성화된 수어 타이밍 범위에 맞춰 버퍼 관리
     */
    fun add(p: FramePack) = synchronized(lock) {
        buf.addLast(p)
        
        // 미디어파이프 좌표 데이터 상세 로깅
        logFramePackDetails(p)
        
        // 현재 활성화된 수어 타이밍이 있으면 해당 범위에 맞춰 버퍼 정리
        currentActionStart?.let { startTime ->
            currentActionEnd?.let { endTime ->
                // 수어 타이밍 범위 + 여유 시간(1초)을 고려한 버퍼 정리
                val bufferStart = startTime - 1000 // 1초 여유
                val bufferEnd = endTime + 1000     // 1초 여유
                
                // 버퍼 범위를 벗어난 오래된 프레임들 제거
                while (buf.isNotEmpty() && buf.first().tsMs < bufferStart) {
                    buf.removeFirst()
                }
                
                Log.d("DynamicLandmarkBuffer", "버퍼 관리: 범위 ${bufferStart}ms ~ ${bufferEnd}ms, 현재 프레임: ${p.tsMs}ms")
            }
        } ?: run {
            // 활성화된 수어 타이밍이 없으면 기본 3초 버퍼 유지
            val cutoff = p.tsMs - 3000
            while (buf.isNotEmpty() && buf.first().tsMs < cutoff) {
                buf.removeFirst()
            }
        }
    }

    /**
     * 수어 타이밍 범위 설정
     * actionStartedAt과 actionEndedAt에 맞춰 버퍼 범위 설정
     */
    fun setActionTiming(actionStartMs: Long, actionEndMs: Long, wordId: String) = synchronized(lock) {
        currentActionStart = actionStartMs
        currentActionEnd = actionEndMs
        currentWordId = wordId
        
        Log.d("DynamicLandmarkBuffer", "수어 타이밍 설정: ${actionStartMs}ms ~ ${actionEndMs}ms, wordId: $wordId")
        
        // 새로운 타이밍에 맞춰 버퍼 정리
        val bufferStart = actionStartMs - 1000 // 1초 여유
        val bufferEnd = actionEndMs + 1000     // 1초 여유
        
        // 범위를 벗어난 프레임들 제거
        while (buf.isNotEmpty() && buf.first().tsMs < bufferStart) {
            buf.removeFirst()
        }
    }

    /**
     * 수어 타이밍 범위 해제
     */
    fun clearActionTiming() = synchronized(lock) {
        currentActionStart = null
        currentActionEnd = null
        currentWordId = null
        
        Log.d("DynamicLandmarkBuffer", "수어 타이밍 해제")
    }

    /**
     * 현재 수어 타이밍 범위 내의 프레임들 추출
     */
    fun sliceForAction(): List<FramePack> = synchronized(lock) {
        currentActionStart?.let { startTime ->
            currentActionEnd?.let { endTime ->
                val frames = buf.filter { it.tsMs in startTime..endTime }
                Log.d("DynamicLandmarkBuffer", "수어 타이밍 프레임 추출: ${frames.size}개 (${startTime}ms ~ ${endTime}ms)")
                return frames
            }
        }
        
        // 활성화된 수어 타이밍이 없으면 빈 리스트 반환
        Log.d("DynamicLandmarkBuffer", "활성화된 수어 타이밍 없음")
        return emptyList()
    }

    /**
     * 특정 시간 중심으로 프레임들 추출 (기존 방식 호환)
     */
    fun sliceAround(centerMs: Long, windowMs: Long = 2000): List<FramePack> = synchronized(lock) {
        val start = centerMs - windowMs / 2
        val end = centerMs + windowMs / 2
        val frames = buf.filter { it.tsMs in start..end }
        
        Log.d("DynamicLandmarkBuffer", "중심 시간 프레임 추출: ${frames.size}개 (${start}ms ~ ${end}ms)")
        return frames
    }

    /**
     * 최신 프레임 시간 반환
     */
    fun latest(): Long = synchronized(lock) { 
        buf.lastOrNull()?.tsMs ?: 0L 
    }

    /**
     * 현재 수어 타이밍이 활성화되어 있는지 확인
     */
    fun isActionActive(): Boolean = synchronized(lock) {
        currentActionStart != null && currentActionEnd != null
    }

    /**
     * 현재 시간이 수어 타이밍 범위 내에 있는지 확인
     */
    fun isInActionRange(currentMs: Long): Boolean = synchronized(lock) {
        currentActionStart?.let { startTime ->
            currentActionEnd?.let { endTime ->
                return currentMs in startTime..endTime
            }
        }
        return false
    }

    /**
     * 버퍼 상태 정보 반환 (디버깅용)
     */
    fun getBufferInfo(): String = synchronized(lock) {
        "Buffer: ${buf.size} frames, Latest: ${latest()}ms, Action: ${currentActionStart}ms ~ ${currentActionEnd}ms"
    }
    
    /**
     * 버퍼에 쌓인 프레임들의 상세 정보 출력 (디버깅용)
     */
    fun logBufferDetails() = synchronized(lock) {
        Log.d("DynamicLandmarkBuffer", "=== 버퍼 상세 정보 ===")
        Log.d("DynamicLandmarkBuffer", "총 프레임 수: ${buf.size}")
        Log.d("DynamicLandmarkBuffer", "수어 타이밍: ${currentActionStart}ms ~ ${currentActionEnd}ms (wordId: $currentWordId)")
        
        if (buf.isNotEmpty()) {
            val firstFrame = buf.first()
            val lastFrame = buf.last()
            Log.d("DynamicLandmarkBuffer", "시간 범위: ${firstFrame.tsMs}ms ~ ${lastFrame.tsMs}ms")
            
            // 최근 3개 프레임의 상세 정보 출력
            val recentFrames = buf.takeLast(3)
            recentFrames.forEachIndexed { index, frame ->
                Log.d("DynamicLandmarkBuffer", "프레임 ${buf.size - recentFrames.size + index + 1}: ${frame.tsMs}ms")
                Log.d("DynamicLandmarkBuffer", "  - 포즈: ${frame.pose.size}개 랜드마크")
                Log.d("DynamicLandmarkBuffer", "  - 왼손: ${frame.left.size}개 랜드마크")
                Log.d("DynamicLandmarkBuffer", "  - 오른손: ${frame.right.size}개 랜드마크")
                
                // 첫 번째 프레임의 샘플 랜드마크 출력
                if (index == 0) {
                    frame.pose.take(3).forEachIndexed { i, lm ->
                        if (lm != null) {
                            Log.d("DynamicLandmarkBuffer", "    포즈[$i]: (${String.format("%.3f", lm.x)}, ${String.format("%.3f", lm.y)}, ${String.format("%.3f", lm.z ?: 0f)})")
                        } else {
                            Log.d("DynamicLandmarkBuffer", "    포즈[$i]: null")
                        }
                    }
                    frame.left.take(3).forEachIndexed { i, lm ->
                        if (lm != null) {
                            Log.d("DynamicLandmarkBuffer", "    왼손[$i]: (${String.format("%.3f", lm.x)}, ${String.format("%.3f", lm.y)}, ${String.format("%.3f", lm.z ?: 0f)})")
                        } else {
                            Log.d("DynamicLandmarkBuffer", "    왼손[$i]: null")
                        }
                    }
                    frame.right.take(3).forEachIndexed { i, lm ->
                        if (lm != null) {
                            Log.d("DynamicLandmarkBuffer", "    오른손[$i]: (${String.format("%.3f", lm.x)}, ${String.format("%.3f", lm.y)}, ${String.format("%.3f", lm.z ?: 0f)})")
                        } else {
                            Log.d("DynamicLandmarkBuffer", "    오른손[$i]: null")
                        }
                    }
                }
            }
        }
        Log.d("DynamicLandmarkBuffer", "========================")
    }
    
    /**
     * FramePack의 상세 좌표 데이터 로깅
     */
    private fun logFramePackDetails(framePack: FramePack) {
        Log.d("MediaPipeCoordinates", "=== 프레임 ${framePack.tsMs}ms 좌표 데이터 ===")
        
        // 포즈 랜드마크 (23개)
        Log.d("MediaPipeCoordinates", "포즈 랜드마크 (${framePack.pose.size}개):")
        framePack.pose.forEachIndexed { index, lm ->
            if (lm != null) {
                Log.d("MediaPipeCoordinates", "  포즈[$index]: (${String.format("%.4f", lm.x)}, ${String.format("%.4f", lm.y)}, ${String.format("%.4f", lm.z ?: 0f)})")
            } else {
                Log.d("MediaPipeCoordinates", "  포즈[$index]: null (인식되지 않음)")
            }
        }
        
        // 왼손 랜드마크 (21개)
        Log.d("MediaPipeCoordinates", "왼손 랜드마크 (${framePack.left.size}개):")
        framePack.left.forEachIndexed { index, lm ->
            if (lm != null) {
                Log.d("MediaPipeCoordinates", "  왼손[$index]: (${String.format("%.4f", lm.x)}, ${String.format("%.4f", lm.y)}, ${String.format("%.4f", lm.z ?: 0f)})")
            } else {
                Log.d("MediaPipeCoordinates", "  왼손[$index]: null (인식되지 않음)")
            }
        }
        
        // 오른손 랜드마크 (21개)
        Log.d("MediaPipeCoordinates", "오른손 랜드마크 (${framePack.right.size}개):")
        framePack.right.forEachIndexed { index, lm ->
            if (lm != null) {
                Log.d("MediaPipeCoordinates", "  오른손[$index]: (${String.format("%.4f", lm.x)}, ${String.format("%.4f", lm.y)}, ${String.format("%.4f", lm.z ?: 0f)})")
            } else {
                Log.d("MediaPipeCoordinates", "  오른손[$index]: null (인식되지 않음)")
            }
        }
        
        // 수어 타이밍 상태
        if (isActionActive()) {
            Log.d("MediaPipeCoordinates", "수어 타이밍 활성: ${currentActionStart}ms ~ ${currentActionEnd}ms (wordId: $currentWordId)")
        } else {
            Log.d("MediaPipeCoordinates", "수어 타이밍 비활성")
        }
        
        Log.d("MediaPipeCoordinates", "================================")
    }
    
    /**
     * 특정 시간대의 좌표 데이터를 JSON 형태로 로깅 (API 전송용)
     */
    fun logCoordinatesForUpload(frames: List<FramePack>) {
        Log.d("MediaPipeUpload", "=== 업로드용 좌표 데이터 (${frames.size}개 프레임) ===")
        frames.forEachIndexed { frameIndex, frame ->
            Log.d("MediaPipeUpload", "프레임 $frameIndex (${frame.tsMs}ms):")
            
            // 포즈 데이터
            val poseData = frame.pose.mapIndexed { index, lm ->
                if (lm != null) {
                    "포즈[$index]:(${String.format("%.4f", lm.x)},${String.format("%.4f", lm.y)},${String.format("%.4f", lm.z ?: 0f)})"
                } else {
                    "포즈[$index]:null"
                }
            }.joinToString("|")
            Log.d("MediaPipeUpload", "  포즈: $poseData")
            
            // 왼손 데이터
            val leftHandData = frame.left.mapIndexed { index, lm ->
                if (lm != null) {
                    "왼손[$index]:(${String.format("%.4f", lm.x)},${String.format("%.4f", lm.y)},${String.format("%.4f", lm.z ?: 0f)})"
                } else {
                    "왼손[$index]:null"
                }
            }.joinToString("|")
            Log.d("MediaPipeUpload", "  왼손: $leftHandData")
            
            // 오른손 데이터
            val rightHandData = frame.right.mapIndexed { index, lm ->
                if (lm != null) {
                    "오른손[$index]:(${String.format("%.4f", lm.x)},${String.format("%.4f", lm.y)},${String.format("%.4f", lm.z ?: 0f)})"
                } else {
                    "오른손[$index]:null"
                }
            }.joinToString("|")
            Log.d("MediaPipeUpload", "  오른손: $rightHandData")
        }
        Log.d("MediaPipeUpload", "==========================================")
    }
}
