package com.ssafy.a602.game.play.net

import com.ssafy.a602.game.play.input.*
import com.ssafy.a602.game.play.dto.*
import com.ssafy.a602.game.api.RhythmApi
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
    fun addFrame(pose: List<LM?>, left: List<LM?>, right: List<LM?>) {
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
        // HTTP 모드는 현재 비활성화됨 - WebSocket 모드 사용
        android.util.Log.d("HttpStreamer", "HTTP 모드 비활성화됨 - WebSocket 모드 사용")
        
        // 임시로 PERFECT 판정 반환 (실제로는 WebSocket에서 처리)
        val judgmentResult = WebSocketJudgmentResult(
            judgment = "Perfect",
            points = 100,
            combo = 1,
            totalScore = 100,
            perfectCount = 1,
            goodCount = 0,
            missCount = 0
        )
        onJudgmentReceived?.invoke(judgmentResult)
    }
    
    private fun buildHttpRequest(type: String, timestamp: Long, frames: List<FrameEntry>): SimilarityRequest {
        val frameBlocks = frames.mapIndexed { index, frameEntry ->
            FrameBlock(
                frame = index + 1,
                poses = listOf(
                    PoseBlock(
                        part = "BODY",
                        coordinates = frameEntry.pose.mapNotNull { lm ->
                            lm?.let {
                                Coordinate(
                                    x = it.x ?: 0f,
                                    y = it.y ?: 0f,
                                    z = it.z ?: 0f,
                                    w = it.w ?: 0f
                                )
                            }
                        }
                    ),
                    PoseBlock(
                        part = "LEFT_HAND",
                        coordinates = frameEntry.left.mapNotNull { lm ->
                            lm?.let {
                                Coordinate(
                                    x = it.x ?: 0f,
                                    y = it.y ?: 0f,
                                    z = it.z ?: 0f,
                                    w = it.w ?: 0f
                                )
                            }
                        }
                    ),
                    PoseBlock(
                        part = "RIGHT_HAND",
                        coordinates = frameEntry.right.mapNotNull { lm ->
                            lm?.let {
                                Coordinate(
                                    x = it.x ?: 0f,
                                    y = it.y ?: 0f,
                                    z = it.z ?: 0f,
                                    w = it.w ?: 0f
                                )
                            }
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
