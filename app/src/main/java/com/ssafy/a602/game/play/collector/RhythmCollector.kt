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
    private var currentType = "PLAY"
    private var currentStartMs: Long = 0L
    private val currentFrames = mutableListOf<FrameDto>()
    private var globalFrame = 0
    
    // 300ms 단위 묶음 관리
    private val frameBundles = mutableListOf<FrameBundle>()
    private var currentBundle: MutableList<FrameDto>? = null
    private var currentBundleTimestamp: Long = 0L
    private var globalTimestamp: Long = 0L  // 전역 타임스탬프 (초기화되지 않음)
    
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
        currentType = "PLAY"
        currentStartMs = 0L
        globalFrame = 1
        segments.clear()
        currentFrames.clear()
        frameBundles.clear()
        currentBundle = null
        currentBundleTimestamp = 0L
        
        // 🔥 300ms 주기 타이머 시작
        startPeriodicCollection()
    }
    
    /**
     * 300ms 주기로 프레임 묶음 생성
     */
    private fun startPeriodicCollection() {
        collectionJob = coroutineScope.launch {
            while (isActive && isCollecting) {
                delay(COLLECTION_INTERVAL_MS)
                if (isCollecting) {
                    // 300ms마다 현재 묶음을 완료하고 새 묶음 시작
                    flushCurrentBundle()
                    startNewBundle()
                }
            }
        }
    }
    
    /**
     * 세그먼트 타입 변경 시 호출 (PLAY/PAUSE/RESUME)
     */
    suspend fun onTypeChanged(newType: String, positionMs: Long) {
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
            // 현재 묶음이 없으면 새로 시작
            if (currentBundle == null) {
                startNewBundle()
            }
            
            val frameDto = FrameDto(
                frame = globalFrame++,
                poses = poses
            )
            currentBundle?.add(frameDto)
            
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
            
            // 마지막 묶음과 세그먼트 마감
            flushCurrentBundle()
            flushCurrentSegment()
            
            // 300ms 묶음들을 세그먼트로 변환
            convertBundlesToSegments()
            
            val request = RhythmSaveRequest(
                musicId = musicId,
                allFrames = segments.toList()
            )
            
            Log.d(TAG, "최종 데이터 생성 완료: ${segments.size}개 세그먼트, 총 ${segments.sumOf { it.frames.size }}개 프레임")
            
            // 🔍 최종 JSON 구조 상세 로그
            Log.d(TAG, "🔍 최종 JSON 구조:")
            segments.forEachIndexed { index, segment ->
                Log.d(TAG, "  세그먼트[$index]: type=${segment.type}, timestamp=${segment.timestamp}ms, frames=${segment.frames.size}개")
                segment.frames.take(3).forEachIndexed { frameIndex, frame ->
                    Log.d(TAG, "    프레임[$frameIndex]: frame=${frame.frame}, poses=${frame.poses.size}개")
                    frame.poses.forEachIndexed { poseIndex, pose ->
                        Log.d(TAG, "      포즈[$poseIndex]: part=${pose.part}, coordinates=${pose.coordinates.size}개")
                    }
                }
                if (segment.frames.size > 3) {
                    Log.d(TAG, "    ... (총 ${segment.frames.size}개 프레임)")
                }
            }
            
            return request
        }
    }
    
    /**
     * 새 묶음 시작 (300ms 단위)
     */
    private fun startNewBundle() {
        // 전역 타임스탬프 사용 (초기화되지 않음)
        currentBundleTimestamp = globalTimestamp
        globalTimestamp += 300L  // 다음 묶음을 위해 300ms 증가
        currentBundle = mutableListOf()
        Log.d(TAG, "새 묶음 시작: timestamp=${currentBundleTimestamp}ms (전역: ${globalTimestamp}ms)")
    }
    
    /**
     * 현재 묶음을 완료하고 frameBundles에 추가
     */
    private fun flushCurrentBundle() {
        currentBundle?.let { bundle ->
            if (bundle.isNotEmpty()) {
                val frameBundle = FrameBundle(
                    timestamp = currentBundleTimestamp,
                    frames = bundle.toList()
                )
                frameBundles.add(frameBundle)
                Log.d(TAG, "묶음 완료: timestamp=${currentBundleTimestamp}ms, frames=${bundle.size}개")
            }
            currentBundle = null
        }
    }
    
    /**
     * 300ms 묶음들을 세그먼트로 변환
     */
    private fun convertBundlesToSegments() {
        segments.clear()
        
        // 각 300ms 묶음을 별도의 세그먼트로 변환
        frameBundles.forEach { bundle ->
            if (bundle.frames.isNotEmpty()) {
                val segment = SegmentDto(
                    type = currentType,
                    timestamp = bundle.timestamp, // 0, 300, 600, 900...
                    frames = bundle.frames
                )
                segments.add(segment)
            }
        }
        Log.d(TAG, "묶음들을 세그먼트로 변환: ${frameBundles.size}개 묶음 → ${segments.size}개 세그먼트")
    }
    
    /**
     * 현재 세그먼트를 마감하고 segments에 추가
     */
    private fun flushCurrentSegment() {
        // 이제 convertBundlesToSegments에서 처리하므로 여기서는 빈 구현
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
