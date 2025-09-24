package com.ssafy.a602.game.play.net

import com.ssafy.a602.game.play.input.*
import com.ssafy.a602.game.play.dto.*
import com.ssafy.a602.game.play.api.RhythmApi
import com.ssafy.a602.auth.TokenManager
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpStreamer @Inject constructor(
    private val tokenManager: TokenManager,
    private val rhythmApi: RhythmApi
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var isStreaming = false
    @Volatile private var paused = false

    private val buffer = PingPongBuffer<FrameEntry>(initialCapacity = 10)
    private val windowLocalIndex = AtomicInteger(0)
    private var playerPositionProvider: (() -> Long)? = null
    
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = false
    }
    
    // 판정 결과 수신 콜백
    private var onJudgmentReceived: ((WebSocketJudgmentResult) -> Unit)? = null
    

    fun startStreaming(playerPositionMs: () -> Long, onJudgment: (WebSocketJudgmentResult) -> Unit) {
        playerPositionProvider = playerPositionMs
        this.onJudgmentReceived = onJudgment
        isStreaming = true
        
        // 0.3초마다 HTTP 요청 전송
        scope.launch {
            while (isActive && isStreaming) {
                delay(300L)
                if (paused) { 
                    buffer.swapAndGet().clear()
                    windowLocalIndex.set(0)
                    continue 
                }

                val list = buffer.swapAndGet()
                windowLocalIndex.set(0)

                if (list.isNotEmpty()) {
                    val timestamp = playerPositionProvider?.invoke() ?: 0L
                    sendHttpRequest("PLAY", timestamp, list)
                    list.clear()
                }
            }
        }
    }

    fun stop() {
        isStreaming = false
        scope.cancel()
        buffer.clearAll()
        windowLocalIndex.set(0)
        paused = false
    }

    /** 캡처 콜백에서 호출 */
    fun addFrame(pose: List<LM>, left: List<LM>, right: List<LM>) {
        if (paused) return
        if (pose.size != 23 || left.size != 21 || right.size != 21) return
        
        val idx = windowLocalIndex.getAndIncrement()
        buffer.add(FrameEntry(idx, pose, left, right))
    }

    /** PAUSE/RESUME 액션을 서버로 알림 */
    fun sendPauseResume(pausedNow: Boolean) {
        paused = pausedNow
        windowLocalIndex.set(0)
        
        scope.launch {
            val timestamp = playerPositionProvider?.invoke() ?: 0L
            val action = if (paused) "PAUSE" else "RESUME"
            sendHttpRequest(action, timestamp, emptyList())
        }
    }
    
    private suspend fun sendHttpRequest(type: String, timestamp: Long, frames: List<FrameEntry>) {
        try {
            val request = buildHttpRequest(type, timestamp, frames)
            
            // 토큰 자동 주입이 적용된 API 사용 (AuthInterceptor가 자동으로 토큰 추가)
            val response = rhythmApi.getSimilarity(request, "") // 빈 문자열로 전달 (AuthInterceptor가 처리)
            
            if (response.isSuccessful) {
                val result = response.body()
                result?.let { 
                    // HTTP 응답을 WebSocket 형식으로 변환
                    val judgmentResult = WebSocketJudgmentResult(
                        judgment = "PERFECT", // 서버에서 계산된 유사도에 따라 판정
                        word = "DANCE", // 임시
                        timestamp = it.timestamp,
                        score = (it.similarity * 100).toInt(), // 유사도를 점수로 변환
                        combo = 1, // 임시
                        totalScore = null,
                        maxCombo = null,
                        accuracy = it.similarity,
                        grade = null
                    )
                    onJudgmentReceived?.invoke(judgmentResult)
                }
            } else {
                android.util.Log.e("HttpStreamer", "HTTP 요청 실패: ${response.code()}")
            }
        } catch (e: Exception) {
            android.util.Log.e("HttpStreamer", "HTTP 요청 중 오류 발생", e)
        }
    }
    
    private fun buildHttpRequest(type: String, timestamp: Long, frames: List<FrameEntry>): SimilarityRequest {
        val frameBlocks = frames.mapIndexed { index, frameEntry ->
            FrameBlock(
                frame = index + 1,
                poses = listOf(
                    PoseBlock(
                        part = "BODY",
                        coordinates = frameEntry.pose.map { lm ->
                            Coordinate(
                                x = lm.x ?: 0f,
                                y = lm.y ?: 0f,
                                z = lm.z ?: 0f,
                                w = lm.w ?: 0f
                            )
                        }
                    ),
                    PoseBlock(
                        part = "LEFT_HAND",
                        coordinates = frameEntry.left.map { lm ->
                            Coordinate(
                                x = lm.x ?: 0f,
                                y = lm.y ?: 0f,
                                z = lm.z ?: 0f,
                                w = lm.w ?: 0f
                            )
                        }
                    ),
                    PoseBlock(
                        part = "RIGHT_HAND",
                        coordinates = frameEntry.right.map { lm ->
                            Coordinate(
                                x = lm.x ?: 0f,
                                y = lm.y ?: 0f,
                                z = lm.z ?: 0f,
                                w = lm.w ?: 0f
                            )
                        }
                    )
                )
            )
        }
        
        return SimilarityRequest(
            type = type,
            timestamp = timestamp,
            frames = frameBlocks
        )
    }
}
