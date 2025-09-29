package com.ssafy.a602.game.play.judge

import android.util.Log
import com.ssafy.a602.game.play.input.LM
import kotlin.math.abs

/**
 * 사용자 프레임 특징량을 저장하는 링 버퍼
 * MediaPipe 결과를 실시간으로 수집하고 정규화된 특징량으로 변환
 */
class FeatureRingBuffer(
    private val bufferSize: Int = 100 // 100개 프레임 저장 (약 3초)
) {
    private val TAG = "FeatureRingBuffer"
    
    // 링 버퍼
    private val buffer = Array<FrameFeature?>(bufferSize) { null }
    private var head = 0
    private var size = 0
    
    // 최신 프레임 캐시
    private var latestFrame: FrameFeature? = null
    
    /**
     * MediaPipe 결과를 특징량으로 변환하여 버퍼에 추가
     * @param pose 포즈 랜드마크
     * @param left 왼손 랜드마크
     * @param right 오른손 랜드마크
     * @param timestampMs 타임스탬프 (ms)
     */
    fun addFrame(
        pose: List<LM?>,
        left: List<LM?>,
        right: List<LM?>,
        timestampMs: Long
    ) {
        try {
            val frameFeature = convertToFrameFeature(pose, left, right, timestampMs)
            if (frameFeature != null) {
                addToBuffer(frameFeature)
                latestFrame = frameFeature
            }
        } catch (e: Exception) {
            Log.e(TAG, "프레임 추가 실패", e)
        }
    }
    
    /**
     * MediaPipe 결과를 FrameFeature로 변환
     */
    private fun convertToFrameFeature(
        pose: List<LM?>,
        left: List<LM?>,
        right: List<LM?>,
        timestampMs: Long
    ): FrameFeature? {
        return try {
            val poseFeatures = extractFeatures(pose)
            val leftFeatures = extractFeatures(left)
            val rightFeatures = extractFeatures(right)
            
            FrameFeature(
                timestampMs = timestampMs,
                pose = poseFeatures,
                left = leftFeatures,
                right = rightFeatures
            )
        } catch (e: Exception) {
            Log.e(TAG, "특징량 추출 실패", e)
            null
        }
    }
    
    /**
     * 랜드마크 리스트에서 특징량 추출
     */
    private fun extractFeatures(landmarks: List<LM?>): FloatArray {
        val features = mutableListOf<Float>()
        
        landmarks.forEach { landmark ->
            if (landmark != null) {
                // x, y, z 좌표 추출
                val x = landmark.x ?: 0f
                val y = landmark.y ?: 0f
                val z = landmark.z ?: 0f
                
                // 디버깅: 첫 번째 랜드마크만 로그 출력
                if (features.isEmpty()) {
                    Log.d(TAG, "🔍 첫 번째 랜드마크: x=$x, y=$y, z=$z")
                }
                
                features.add(x)
                features.add(y)
                features.add(z)
            } else {
                // null인 경우 0으로 채움
                features.add(0f)
                features.add(0f)
                features.add(0f)
            }
        }
        
        return features.toFloatArray()
    }
    
    /**
     * 버퍼에 프레임 추가
     */
    private fun addToBuffer(frame: FrameFeature) {
        buffer[head] = frame
        head = (head + 1) % bufferSize
        
        if (size < bufferSize) {
            size++
        }
    }
    
    /**
     * 현재 시간에 가장 가까운 프레임 반환
     * @param currentTimeMs 현재 시간 (ms)
     * @return 가장 가까운 프레임 또는 null
     */
    fun getNearestFrame(currentTimeMs: Long): FrameFeature? {
        if (size == 0) return null
        
        var nearestFrame: FrameFeature? = null
        var minTimeDiff = Long.MAX_VALUE
        
        for (i in 0 until size) {
            val frame = buffer[i] ?: continue
            val timeDiff = abs(frame.timestampMs - currentTimeMs)
            
            if (timeDiff < minTimeDiff) {
                minTimeDiff = timeDiff
                nearestFrame = frame
            }
        }
        
        return nearestFrame
    }
    
    /**
     * 최신 프레임 반환
     * @return 최신 프레임 또는 null
     */
    fun getLatestFrame(): FrameFeature? {
        return latestFrame
    }
    
    /**
     * 현재 시간에 가장 가까운 프레임 반환 (최신 프레임 우선)
     * @param currentTimeMs 현재 시간 (ms)
     * @return 가장 가까운 프레임 또는 최신 프레임
     */
    fun getLatestOrNearest(currentTimeMs: Long): FrameFeature? {
        val nearest = getNearestFrame(currentTimeMs)
        return nearest ?: getLatestFrame()
    }
    
    /**
     * 버퍼 상태 정보 반환
     */
    fun getBufferInfo(): String {
        return "FeatureRingBuffer(size=$size, head=$head, latest=${latestFrame?.timestampMs})"
    }
    
    /**
     * 버퍼 초기화
     */
    fun clear() {
        buffer.fill(null)
        head = 0
        size = 0
        latestFrame = null
        Log.d(TAG, "버퍼 초기화됨")
    }
    
    /**
     * 버퍼 크기 반환
     */
    fun getSize(): Int = size
    
    /**
     * 버퍼가 비어있는지 확인
     */
    fun isEmpty(): Boolean = size == 0
}
