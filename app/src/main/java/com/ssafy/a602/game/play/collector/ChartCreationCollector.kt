package com.ssafy.a602.game.play.collector

import android.content.Context
import android.util.Log
import com.ssafy.a602.game.play.dto.*
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectOutputStream
import java.io.Serializable

/**
 * 채보만들기 모드용 프레임 수집기
 * 모든 프레임을 원본 이미지로 저장한 후, 곡 종료 시 일괄 MediaPipe 처리
 */
class ChartCreationCollector(
    private val musicId: Int,
    private val coroutineScope: CoroutineScope,
    private val context: Context? = null
) {
    private val mutex = Mutex()
    
    // 원본 프레임 데이터 저장 (메모리 최적화를 위해 파일 기반 저장)
    private val rawFrames = mutableListOf<RawFrameData>()
    private var isCollecting = false
    private var globalFrameIndex = 0
    
    // MediaPipe 처리용 랜드마커들
    private var poseLandmarker: PoseLandmarker? = null
    private var handLandmarker: HandLandmarker? = null
    
    // 메모리 최적화를 위한 임시 파일 저장
    private val tempFiles = mutableListOf<File>()
    private var tempDir: File? = null
    private val MAX_MEMORY_FRAMES = 100 // 메모리에 보관할 최대 프레임 수
    
    companion object {
        private const val TAG = "ChartCreationCollector"
    }
    
    /**
     * 게임 시작 시 호출
     */
    fun startCollection() {
        Log.d(TAG, "채보만들기 프레임 수집 시작: musicId=$musicId")
        isCollecting = true
        globalFrameIndex = 0
        rawFrames.clear()
        tempFiles.clear()
        
        // 임시 디렉토리 생성
        createTempDirectory()
        
        // MediaPipe 랜드마커 초기화
        initMediaPipeLandmarkers()
    }
    
    /**
     * 임시 디렉토리 생성
     */
    private fun createTempDirectory() {
        try {
            tempDir = File(context?.cacheDir, "chart_creation_$musicId")
            tempDir?.mkdirs()
            Log.d(TAG, "임시 디렉토리 생성: ${tempDir?.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "임시 디렉토리 생성 실패", e)
        }
    }
    
    /**
     * MediaPipe 랜드마커 초기화
     */
    private fun initMediaPipeLandmarkers() {
        if (context == null) {
            Log.w(TAG, "Context가 null이므로 MediaPipe 초기화 건너뛰기")
            return
        }
        
        try {
            Log.d(TAG, "MediaPipe 랜드마커 초기화 시작")
            
            // Pose Landmarker 초기화
            val poseBaseOptions = BaseOptions.builder()
                .setModelAssetPath("models/pose_landmarker_lite.task")
                .build()
            
            val poseOptions = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(poseBaseOptions)
                .setRunningMode(RunningMode.IMAGE) // 이미지 모드 사용
                .build()
            
            poseLandmarker = PoseLandmarker.createFromOptions(context, poseOptions)
            Log.d(TAG, "Pose Landmarker 초기화 완료")
            
            // Hand Landmarker 초기화
            val handBaseOptions = BaseOptions.builder()
                .setModelAssetPath("models/hand_landmarker.task")
                .build()
            
            val handOptions = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(handBaseOptions)
                .setNumHands(2)
                .setMinHandDetectionConfidence(0.3f)
                .setMinHandPresenceConfidence(0.3f)
                .setMinTrackingConfidence(0.3f)
                .setRunningMode(RunningMode.IMAGE) // 이미지 모드 사용
                .build()
            
            handLandmarker = HandLandmarker.createFromOptions(context, handOptions)
            Log.d(TAG, "Hand Landmarker 초기화 완료")
            
        } catch (e: Exception) {
            Log.e(TAG, "MediaPipe 랜드마커 초기화 실패", e)
        }
    }
    
    /**
     * 원본 프레임 데이터 저장 (MediaPipe 처리 전)
     * 메모리 최적화: 임계치 초과 시 파일로 저장
     */
    suspend fun addRawFrame(
        imageData: ByteArray,
        timestampMs: Long,
        width: Int,
        height: Int
    ) {
        if (!isCollecting) return
        
        mutex.withLock {
            val frameData = RawFrameData(
                frameIndex = globalFrameIndex++,
                imageData = imageData,
                timestampMs = timestampMs,
                width = width,
                height = height
            )
            
            // 메모리 임계치 확인
            if (rawFrames.size >= MAX_MEMORY_FRAMES) {
                // 메모리에서 파일로 이동
                saveFramesToFile()
                rawFrames.clear()
            }
            
            rawFrames.add(frameData)
            
            // 프레임 수집 통계 로깅 (1초마다)
            if (frameData.frameIndex % 30 == 0) { // 30fps 기준 1초마다
                Log.d(TAG, "프레임 수집 통계: frame=${frameData.frameIndex}, timestamp=${timestampMs}ms, 메모리프레임=${rawFrames.size}, 파일수=${tempFiles.size}")
            } else {
                Log.v(TAG, "원본 프레임 저장: frame=${frameData.frameIndex}, timestamp=${timestampMs}ms, size=${imageData.size}bytes, 메모리프레임=${rawFrames.size}")
            }
        }
    }
    
    /**
     * 현재 메모리의 프레임들을 파일로 저장
     */
    private fun saveFramesToFile() {
        if (rawFrames.isEmpty() || tempDir == null) return
        
        try {
            val file = File(tempDir, "frames_${System.currentTimeMillis()}.dat")
            val outputStream = ObjectOutputStream(FileOutputStream(file))
            
            // 프레임 데이터 직렬화하여 저장
            outputStream.writeObject(rawFrames.toList())
            outputStream.close()
            
            tempFiles.add(file)
            Log.d(TAG, "프레임 파일 저장: ${file.name}, 프레임수=${rawFrames.size}")
            
        } catch (e: Exception) {
            Log.e(TAG, "프레임 파일 저장 실패", e)
        }
    }
    
    /**
     * 곡 종료 시 모든 프레임에 대해 MediaPipe 처리 후 최종 데이터 반환
     */
    suspend fun processAllFramesAndCreateRequest(): RhythmSaveRequest {
        Log.d(TAG, "채보만들기 일괄 처리 시작: 메모리프레임=${rawFrames.size}개, 파일=${tempFiles.size}개")
        
        mutex.withLock {
            isCollecting = false
            
            // 1. 모든 프레임 수집 (메모리 + 파일)
            val allFrames = collectAllFrames()
            
            // 2. 모든 프레임을 300ms 단위로 그룹화
            val frameGroups = groupFramesBy300ms(allFrames)
            
            // 타임스탬프 그룹 로그 출력
            Log.d(TAG, "타임스탬프 그룹: ${frameGroups.keys.sorted()}")
            frameGroups.forEach { (timestamp, frames) ->
                Log.d(TAG, "  ${timestamp}ms: ${frames.size}개 프레임")
            }
            
            // 3. 각 그룹에 대해 MediaPipe 처리
            val segments = mutableListOf<SegmentDto>()
            
            frameGroups.forEach { (timestampMs, frames) ->
                val processedFrames = mutableListOf<FrameDto>()
                
                // 실제 프레임이 있는 경우만 MediaPipe 처리
                frames.forEach { rawFrame ->
                    val poses = processFrameWithMediaPipe(rawFrame)
                    val frameDto = FrameDto(
                        frame = rawFrame.frameIndex,
                        poses = poses
                    )
                    processedFrames.add(frameDto)
                }
                
                // 프레임이 있는 구간만 세그먼트로 포함
                if (processedFrames.isNotEmpty()) {
                    val segment = SegmentDto(
                        type = "PLAY",
                        timestamp = timestampMs,
                        frames = processedFrames
                    )
                    segments.add(segment)
                }
            }
            
            Log.d(TAG, "일괄 처리 완료: ${segments.size}개 세그먼트, 총 ${segments.sumOf { it.frames.size }}개 프레임")
            
            // 4. 임시 파일 정리
            cleanupTempFiles()
            
            return RhythmSaveRequest(
                musicId = musicId,
                allFrames = segments
            )
        }
    }
    
    /**
     * 모든 프레임 수집 (메모리 + 파일)
     */
    private fun collectAllFrames(): List<RawFrameData> {
        val allFrames = mutableListOf<RawFrameData>()
        
        // 메모리의 프레임들 추가
        allFrames.addAll(rawFrames)
        
        // 파일의 프레임들 로드
        tempFiles.forEach { file ->
            try {
                val inputStream = java.io.ObjectInputStream(java.io.FileInputStream(file))
                @Suppress("UNCHECKED_CAST")
                val frames = inputStream.readObject() as List<RawFrameData>
                allFrames.addAll(frames)
                inputStream.close()
                Log.d(TAG, "파일에서 프레임 로드: ${file.name}, 프레임수=${frames.size}")
            } catch (e: Exception) {
                Log.e(TAG, "파일에서 프레임 로드 실패: ${file.name}", e)
            }
        }
        
        // 프레임 인덱스 순으로 정렬
        allFrames.sortBy { it.frameIndex }
        
        Log.d(TAG, "전체 프레임 수집 완료: 총 ${allFrames.size}개")
        return allFrames
    }
    
    /**
     * 임시 파일 정리
     */
    private fun cleanupTempFiles() {
        try {
            tempFiles.forEach { file ->
                if (file.exists()) {
                    file.delete()
                }
            }
            tempFiles.clear()
            
            tempDir?.let { dir ->
                if (dir.exists()) {
                    dir.deleteRecursively()
                }
            }
            
            Log.d(TAG, "임시 파일 정리 완료")
        } catch (e: Exception) {
            Log.e(TAG, "임시 파일 정리 실패", e)
        }
    }
    
    /**
     * 프레임들을 300ms 단위로 그룹화
     * 첫 번째 프레임을 0ms로 기준으로 상대적 타임스탬프 계산
     */
    private fun groupFramesBy300ms(frames: List<RawFrameData>): Map<Long, List<RawFrameData>> {
        if (frames.isEmpty()) return emptyMap()
        
        // 첫 번째 프레임의 타임스탬프를 기준으로 상대적 타임스탬프 계산
        val firstTimestamp = frames.minOf { it.timestampMs }
        val lastTimestamp = frames.maxOf { it.timestampMs }
        
        Log.d(TAG, "그룹화 시작: 총 ${frames.size}개 프레임, 시간범위: ${firstTimestamp}ms ~ ${lastTimestamp}ms")
        
        val groupedFrames = frames.groupBy { frame ->
            val relativeTimestamp = frame.timestampMs - firstTimestamp
            (relativeTimestamp / 300) * 300 // 0, 300, 600, 900... 형태로 버킷팅
        }
        
        // 누락된 300ms 구간들을 빈 리스트로 채우기
        val maxTimestamp = groupedFrames.keys.maxOrNull() ?: 0L
        val completeGroups = mutableMapOf<Long, List<RawFrameData>>()
        
        for (timestamp in 0L..maxTimestamp step 300L) {
            val framesInGroup = groupedFrames[timestamp] ?: emptyList()
            completeGroups[timestamp] = framesInGroup
            
            if (framesInGroup.isEmpty()) {
                Log.d(TAG, "빈 구간 발견: timestamp=${timestamp}ms (${timestamp/300}번째 구간)")
            } else {
                Log.d(TAG, "프레임 그룹: timestamp=${timestamp}ms, 프레임수=${framesInGroup.size}")
            }
        }
        
        Log.d(TAG, "그룹화 완료: 총 ${completeGroups.size}개 구간, 빈구간=${completeGroups.values.count { it.isEmpty() }}개")
        return completeGroups
    }
    
    /**
     * 개별 프레임에 대해 MediaPipe 처리
     * 실제 MediaPipe 처리 로직 구현
     */
    private suspend fun processFrameWithMediaPipe(rawFrame: RawFrameData): List<PoseDto> {
        Log.d(TAG, "MediaPipe 처리: frame=${rawFrame.frameIndex}, timestamp=${rawFrame.timestampMs}ms")
        
        try {
            // 1. ByteArray를 Bitmap으로 변환
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(
                rawFrame.imageData, 0, rawFrame.imageData.size
            )
            
            if (bitmap == null) {
                Log.w(TAG, "Bitmap 변환 실패: frame=${rawFrame.frameIndex}")
                return emptyList()
            }
            
            // 2. MediaPipe 이미지 생성
            val mpImage = BitmapImageBuilder(bitmap).build()
            
            // 3. MediaPipe 처리
            val poses = mutableListOf<PoseDto>()
            
            // Pose 처리
            if (poseLandmarker != null) {
                try {
                    val poseResult = poseLandmarker!!.detect(mpImage)
                    if (poseResult.landmarks().isNotEmpty()) {
                        val poseLandmarks = poseResult.landmarks().first()
                        val coordinates = poseLandmarks.map { landmark ->
                            CoordinateDto(
                                x = landmark.x(),
                                y = landmark.y(),
                                z = landmark.z(),
                                w = landmark.visibility().orElse(null)
                            )
                        }
                        
                        poses.add(
                            PoseDto(
                                part = "BODY",
                                coordinates = coordinates
                            )
                        )
                        Log.d(TAG, "BODY 포즈 감지: frame=${rawFrame.frameIndex}, landmarks=${poseLandmarks.size}개")
                    } else {
                        Log.w(TAG, "BODY 포즈 감지 실패: frame=${rawFrame.frameIndex}")
                        // BODY 포즈가 감지되지 않아도 빈 좌표로 추가
                        poses.add(
                            PoseDto(
                                part = "BODY",
                                coordinates = (0..22).map { 
                                    CoordinateDto(x = null, y = null, z = null, w = null)
                                }
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Pose 처리 실패: frame=${rawFrame.frameIndex}", e)
                    // BODY 포즈 처리 실패 시 빈 좌표로 추가
                    poses.add(
                        PoseDto(
                            part = "BODY",
                            coordinates = (0..22).map { 
                                CoordinateDto(x = null, y = null, z = null, w = null)
                            }
                        )
                    )
                }
            } else {
                Log.w(TAG, "PoseLandmarker가 null: frame=${rawFrame.frameIndex}")
                // PoseLandmarker가 없어도 빈 좌표로 추가
                poses.add(
                    PoseDto(
                        part = "BODY",
                        coordinates = (0..22).map { 
                            CoordinateDto(x = null, y = null, z = null, w = null)
                        }
                    )
                )
            }
            
            // Hand 처리
            if (handLandmarker != null) {
                try {
                    val handResult = handLandmarker!!.detect(mpImage)
                    val hands = handResult.landmarks()
                    val handedness = handResult.handednesses()
                    
                    // 왼손 처리
                    var leftHandFound = false
                    var rightHandFound = false
                    
                    for (i in hands.indices) {
                        val handLandmarks = hands[i]
                        val handLabel = handedness.getOrNull(i)?.firstOrNull()?.categoryName() ?: "Unknown"
                        
                        val coordinates = handLandmarks.map { landmark ->
                            CoordinateDto(
                                x = landmark.x(),
                                y = landmark.y(),
                                z = landmark.z(),
                                w = landmark.visibility().orElse(null)
                            )
                        }
                        
                        when (handLabel) {
                            "Left" -> {
                                poses.add(
                                    PoseDto(
                                        part = "LEFT_HAND",
                                        coordinates = coordinates
                                    )
                                )
                                leftHandFound = true
                            }
                            "Right" -> {
                                poses.add(
                                    PoseDto(
                                        part = "RIGHT_HAND",
                                        coordinates = coordinates
                                    )
                                )
                                rightHandFound = true
                            }
                        }
                    }
                    
                    // 손이 감지되지 않은 경우 빈 좌표로 추가
                    if (!leftHandFound) {
                        poses.add(
                            PoseDto(
                                part = "LEFT_HAND",
                                coordinates = (0..20).map { 
                                    CoordinateDto(x = null, y = null, z = null, w = null)
                                }
                            )
                        )
                    }
                    
                    if (!rightHandFound) {
                        poses.add(
                            PoseDto(
                                part = "RIGHT_HAND",
                                coordinates = (0..20).map { 
                                    CoordinateDto(x = null, y = null, z = null, w = null)
                                }
                            )
                        )
                    }
                    
                } catch (e: Exception) {
                    Log.w(TAG, "Hand 처리 실패: frame=${rawFrame.frameIndex}", e)
                    
                    // 손 처리 실패 시 빈 좌표로 추가
                    poses.add(
                        PoseDto(
                            part = "LEFT_HAND",
                            coordinates = (0..20).map { 
                                CoordinateDto(x = null, y = null, z = null, w = null)
                            }
                        )
                    )
                    poses.add(
                        PoseDto(
                            part = "RIGHT_HAND",
                            coordinates = (0..20).map { 
                                CoordinateDto(x = null, y = null, z = null, w = null)
                            }
                        )
                    )
                }
            }
            
            Log.d(TAG, "MediaPipe 처리 완료: frame=${rawFrame.frameIndex}, poses=${poses.size}개")
            return poses
            
        } catch (e: Exception) {
            Log.e(TAG, "MediaPipe 처리 중 오류: frame=${rawFrame.frameIndex}", e)
            return emptyList()
        }
    }
    
    /**
     * 수집 중단
     */
    fun stopCollection() {
        Log.d(TAG, "채보만들기 프레임 수집 중단")
        isCollecting = false
        
        // MediaPipe 리소스 해제
        poseLandmarker?.close()
        handLandmarker?.close()
        poseLandmarker = null
        handLandmarker = null
        
        // 임시 파일 정리
        cleanupTempFiles()
    }
    
    /**
     * 현재 수집 상태 반환
     */
    fun getCollectionInfo(): String {
        return "수집중: $isCollecting, 프레임: ${rawFrames.size}개"
    }
}

/**
 * 원본 프레임 데이터
 */
data class RawFrameData(
    val frameIndex: Int,
    val imageData: ByteArray,
    val timestampMs: Long,
    val width: Int,
    val height: Int
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RawFrameData

        if (frameIndex != other.frameIndex) return false
        if (!imageData.contentEquals(other.imageData)) return false
        if (timestampMs != other.timestampMs) return false
        if (width != other.width) return false
        if (height != other.height) return false

        return true
    }

    override fun hashCode(): Int {
        var result = frameIndex
        result = 31 * result + imageData.contentHashCode()
        result = 31 * result + timestampMs.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        return result
    }
}
