package com.ssafy.a602.game.play.collector

import android.util.Log
import com.ssafy.a602.game.play.dto.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 하드 모드 리듬게임 데이터 수집기
 * 300ms 주기로 프레임을 수집하고, 세그먼트 단위로 관리
 */
class RhythmCollector(
    private val musicId: Int,
    private val coroutineScope: CoroutineScope
) {
    private val mutex = Mutex()
    
    // 세그먼트 관리
    private val segments = mutableListOf<SegmentDto>()
    private var currentType = SegmentType.PLAY
    private var currentStartMs: Long = 0L
    private val currentFrames = mutableListOf<FrameDto>()
    private var globalFrame = 0
    
    // 수집 상태
    private var isCollecting = false
    private var collectionJob: kotlinx.coroutines.Job? = null
    
    companion object {
        private const val TAG = "RhythmCollector"
        private const val COLLECTION_INTERVAL_MS = 300L // 300ms 주기
    }
    
    /**
     * 게임 시작 시 호출
     */
    fun startCollection() {
        Log.d(TAG, "리듬 수집 시작: musicId=$musicId")
        isCollecting = true
        currentType = SegmentType.PLAY
        currentStartMs = 0L
        globalFrame = 0
        segments.clear()
        currentFrames.clear()
        
        // 🔥 300ms 주기 타이머 시작
        startPeriodicCollection()
    }
    
    /**
     * 300ms 주기로 세그먼트 타임스탬프 관리 (실제 수집은 즉시)
     */
    private fun startPeriodicCollection() {
        collectionJob = coroutineScope.launch {
            while (isActive && isCollecting) {
                delay(COLLECTION_INTERVAL_MS)
                if (isCollecting) {
                    // 300ms마다 로그만 출력 (실제 수집은 즉시)
                    Log.v(TAG, "300ms 주기 체크: 현재 세그먼트 ${currentFrames.size}개 프레임")
                }
            }
        }
    }
    
    /**
     * 세그먼트 타입 변경 시 호출 (PLAY/PAUSE/RESUME)
     */
    suspend fun onTypeChanged(newType: SegmentType, positionMs: Long) {
        if (!isCollecting) return
        
        mutex.withLock {
            if (newType == currentType) return@withLock
            
            Log.d(TAG, "세그먼트 타입 변경: $currentType -> $newType at ${positionMs}ms")
            
            // 이전 세그먼트 마감
            flushCurrentSegment()
            
            // 새 세그먼트 시작
            currentType = newType
            currentStartMs = positionMs
        }
    }
    
    /**
     * MediaPipe 결과를 즉시 수집 (모든 프레임 수집)
     */
    suspend fun addFrameToBuffer(poses: List<PoseDto>, positionMs: Long) {
        if (!isCollecting) return
        
        mutex.withLock {
            val frameDto = FrameDto(
                frame = globalFrame++,
                poses = poses
            )
            currentFrames.add(frameDto)
            
            Log.v(TAG, "프레임 수집: frame=${frameDto.frame}, poses=${poses.size}개, position=${positionMs}ms")
        }
    }
    
    /**
     * 게임 종료 시 최종 데이터 반환
     */
    suspend fun onSongEnd(): RhythmSaveRequest {
        Log.d(TAG, "곡 종료 - 최종 데이터 생성")
        
        mutex.withLock {
            isCollecting = false
            collectionJob?.cancel()
            collectionJob = null
            
            // 마지막 세그먼트 마감
            flushCurrentSegment()
            
            val request = RhythmSaveRequest(
                musicId = musicId,
                allFrames = segments.toList()
            )
            
            Log.d(TAG, "최종 데이터 생성 완료: ${segments.size}개 세그먼트, 총 ${segments.sumOf { it.frames.size }}개 프레임")
            return request
        }
    }
    
    /**
     * 현재 세그먼트를 마감하고 segments에 추가
     */
    private fun flushCurrentSegment() {
        val segment = SegmentDto(
            type = currentType,
            timestamp = currentStartMs,
            frames = currentFrames.toList()
        )
        segments.add(segment)
        currentFrames.clear()
        
        Log.d(TAG, "세그먼트 마감: type=$currentType, timestamp=${currentStartMs}ms, frames=${segment.frames.size}개")
    }
    
    /**
     * 수집 중단
     */
    fun stopCollection() {
        Log.d(TAG, "리듬 수집 중단")
        isCollecting = false
        collectionJob?.cancel()
        collectionJob = null
    }
    
    /**
     * 현재 수집 상태 반환
     */
    fun getCollectionInfo(): String {
        return "수집중: $isCollecting, 세그먼트: ${segments.size}개, 현재프레임: ${currentFrames.size}개"
    }
}
