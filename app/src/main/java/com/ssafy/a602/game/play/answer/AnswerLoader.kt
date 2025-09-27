package com.ssafy.a602.game.play.answer

import android.content.Context
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * 정답 JSON 데이터 구조
 */
@kotlinx.serialization.Serializable
data class AnswerJson(
    val musicId: Long,
    val allFrames: List<AnswerFrameJson>
)

@kotlinx.serialization.Serializable
data class AnswerFrameJson(
    val type: String,
    val timestamp: Long,
    val frames: List<FrameDataJson>
)

@kotlinx.serialization.Serializable
data class FrameDataJson(
    val frame: Int,
    val poses: List<PoseJson>
)

@kotlinx.serialization.Serializable
data class PoseJson(
    val part: String,
    val coordinates: List<Float>
)

/**
 * 정답 데이터 로더
 */
object AnswerLoader {
    private const val TAG = "AnswerLoader"
    
    /**
     * assets에서 정답 JSON을 로드하고 전처리된 AnswerTimeline을 반환
     * @param context 컨텍스트
     * @param musicId 음악 ID
     * @return AnswerTimeline 또는 null
     */
    fun load(context: Context, musicId: Long): AnswerTimeline? {
        return try {
            val jsonString = loadJsonFromAssets(context, musicId)
            if (jsonString == null) {
                Log.e(TAG, "JSON 파일을 찾을 수 없음: musicId=$musicId")
                return null
            }
            
            val answerJson = Json.decodeFromString<AnswerJson>(jsonString)
            processAnswerJson(answerJson)
        } catch (e: Exception) {
            Log.e(TAG, "정답 데이터 로드 실패: musicId=$musicId", e)
            null
        }
    }
    
    /**
     * assets에서 JSON 파일 로드
     */
    private fun loadJsonFromAssets(context: Context, musicId: Long): String? {
        return try {
            val fileName = "charts/answer_${musicId}.json"
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            Log.e(TAG, "assets 파일 로드 실패: charts/answer_${musicId}.json", e)
            null
        }
    }
    
    /**
     * JSON 데이터를 AnswerTimeline으로 변환
     */
    private fun processAnswerJson(answerJson: AnswerJson): AnswerTimeline? {
        try {
            // PLAY 타입 세그먼트만 필터링
            val playFrames = answerJson.allFrames.filter { it.type == "PLAY" }
            if (playFrames.isEmpty()) {
                Log.e(TAG, "PLAY 타입 프레임이 없음")
                return null
            }
            
            // 시간순으로 정렬
            val sortedFrames = playFrames.sortedBy { it.timestamp }
            val startMs = sortedFrames.first().timestamp
            
            // 각 프레임을 AnswerFrame으로 변환
            val answerFrames = sortedFrames.mapIndexed { index, frameJson ->
                val processedFrame = processFrame(frameJson, index)
                if (processedFrame == null) {
                    Log.w(TAG, "프레임 처리 실패: index=$index, timestamp=${frameJson.timestamp}")
                }
                processedFrame
            }.filterNotNull()
            
            if (answerFrames.isEmpty()) {
                Log.e(TAG, "처리된 프레임이 없음")
                return null
            }
            
            return AnswerTimeline(
                musicId = answerJson.musicId,
                startMs = startMs,
                frames = answerFrames,
                answerHash = null, // TODO: 필요시 추가
                judgeVersion = null // TODO: 필요시 추가
            )
        } catch (e: Exception) {
            Log.e(TAG, "AnswerTimeline 변환 실패", e)
            return null
        }
    }
    
    /**
     * 단일 프레임 데이터 처리
     */
    private fun processFrame(frameJson: AnswerFrameJson, index: Int): AnswerFrame? {
        return try {
            // 각 프레임의 poses를 특징량으로 변환
            val frameData = frameJson.frames.firstOrNull()
            if (frameData == null) {
                Log.w(TAG, "프레임 데이터가 없음: index=$index")
                return null
            }
            
            val poses = frameData.poses
            val poseFeatures = extractPoseFeatures(poses, "BODY")
            val leftFeatures = extractPoseFeatures(poses, "LEFT_HAND")
            val rightFeatures = extractPoseFeatures(poses, "RIGHT_HAND")
            
            AnswerFrame(
                index = index,
                pose = poseFeatures,
                left = leftFeatures,
                right = rightFeatures
            )
        } catch (e: Exception) {
            Log.e(TAG, "프레임 처리 실패: index=$index", e)
            null
        }
    }
    
    /**
     * 특정 부위의 특징량 추출
     */
    private fun extractPoseFeatures(poses: List<PoseJson>, part: String): FloatArray {
        val targetPose = poses.find { it.part == part }
        return if (targetPose != null && targetPose.coordinates.size >= 3) {
            // 좌표 (x, y, z)만 추출하여 평탄화
            val coords = targetPose.coordinates
            floatArrayOf(
                coords.getOrNull(0) ?: 0f,
                coords.getOrNull(1) ?: 0f,
                coords.getOrNull(2) ?: 0f
            )
        } else {
            // 데이터가 없으면 0으로 채움
            floatArrayOf(0f, 0f, 0f)
        }
    }
}
