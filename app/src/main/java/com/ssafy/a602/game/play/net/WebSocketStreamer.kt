package com.ssafy.a602.game.play.net

import com.ssafy.a602.game.play.input.*
import com.ssafy.a602.game.play.dto.*
import com.ssafy.a602.auth.TokenManager
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import okio.ByteString
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketStreamer @Inject constructor(
    private val httpStreamer: HttpStreamer,
    private val tokenManager: TokenManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var socket: WebSocket? = null
    @Volatile private var connected = false
    @Volatile private var paused = false
    @Volatile private var useHttpMode = false // 웹소켓 모드로 시작

    private val buffer = PingPongBuffer<FrameEntry>(initialCapacity = 10)
    private val windowLocalIndex = AtomicInteger(0)
    private var playerPositionProvider: (() -> Long)? = null
    
    // OkHttpClient 재사용
    private val client by lazy { OkHttpClient() }
    
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = false
    }
    
    // 판정 결과 수신 콜백
    private var onJudgmentReceived: ((WebSocketJudgmentResult) -> Unit)? = null

    fun connect(url: String, playerPositionMs: () -> Long, onJudgment: (WebSocketJudgmentResult) -> Unit) {
        playerPositionProvider = playerPositionMs
        this.onJudgmentReceived = onJudgment
        
        if (useHttpMode) {
            // HTTP 모드 사용
            httpStreamer.startStreaming(playerPositionMs, onJudgment)
            return
        }
        
        // WebSocket 모드 사용
        if (socket != null) return
        
        // 토큰 가져오기
        val token = tokenManager.getAccessToken()
        if (token.isNullOrEmpty()) {
            android.util.Log.e("WebSocketStreamer", "인증 토큰이 없습니다")
            return
        }
        
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()
            
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                connected = true
                android.util.Log.d("WebSocketStreamer", "웹소켓 연결 성공")
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    android.util.Log.d("WebSocketStreamer", "웹소켓 메시지 수신: $text")
                    
                    // 먼저 WebSocketJudgmentResult로 파싱 시도
                    try {
                        val judgmentResult = json.decodeFromString<WebSocketJudgmentResult>(text)
                        android.util.Log.d("WebSocketStreamer", "판정 결과 수신: ${judgmentResult.judgment}")
                        onJudgmentReceived?.invoke(judgmentResult)
                        return
                    } catch (e: Exception) {
                        android.util.Log.d("WebSocketStreamer", "WebSocketJudgmentResult 파싱 실패, SimilarityResponse 시도")
                    }
                    
                    // SimilarityResponse로 파싱 시도
                    try {
                        val result = json.decodeFromString<SimilarityResponse>(text)
                        // SimilarityResponse를 WebSocketJudgmentResult로 변환
                        val judgmentResult = WebSocketJudgmentResult(
                            judgment = when {
                                result.similarity >= 0.9f -> "PERFECT"
                                result.similarity >= 0.7f -> "GREAT"
                                result.similarity >= 0.5f -> "GOOD"
                                else -> "MISS"
                            },
                            word = "DANCE", // TODO: 서버에서 실제 단어 정보 제공
                            timestamp = result.timestamp,
                            score = (result.similarity * 100).toInt(),
                            combo = 1, // TODO: 서버에서 실제 콤보 정보 제공
                            totalScore = null,
                            maxCombo = null,
                            accuracy = result.similarity,
                            grade = when {
                                result.similarity >= 0.9f -> "S"
                                result.similarity >= 0.8f -> "A"
                                result.similarity >= 0.7f -> "B"
                                result.similarity >= 0.6f -> "C"
                                else -> "F"
                            }
                        )
                        android.util.Log.d("WebSocketStreamer", "유사도 기반 판정: ${judgmentResult.judgment} (${result.similarity})")
                        onJudgmentReceived?.invoke(judgmentResult)
                    } catch (e: Exception) {
                        android.util.Log.e("WebSocketStreamer", "SimilarityResponse 파싱 실패", e)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("WebSocketStreamer", "웹소켓 메시지 파싱 실패: $text", e)
                }
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                connected = false
                socket = null
                android.util.Log.e("WebSocketStreamer", "웹소켓 연결 실패", t)
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                connected = false
                socket = null
                android.util.Log.d("WebSocketStreamer", "웹소켓 연결 종료: $code - $reason")
            }
        })
    }

    /** 0.3초마다 PLAY 배치 전송 */
    fun startStreaming() {
        if (useHttpMode) {
            // HTTP 모드에서는 httpStreamer가 처리
            return
        }
        
        scope.launch {
            while (isActive) {
                delay(300L)
                if (!connected) { 
                    buffer.swapAndGet().clear()
                    windowLocalIndex.set(0)
                    continue 
                }
                if (paused) { 
                    buffer.swapAndGet().clear()
                    windowLocalIndex.set(0)
                    continue 
                }

                val list = buffer.swapAndGet()
                // 다음 윈도우부터 인덱스 0부터
                windowLocalIndex.set(0)

                if (list.isNotEmpty()) {
                    val t = playerPositionProvider?.invoke() ?: 0L
                    val request = buildSimilarityRequest("PLAY", t, list.toList())
                    val payload = json.encodeToString(SimilarityRequest.serializer(), request)
                    socket?.send(payload)
                    list.clear()
                }
            }
        }
    }

    fun stop() {
        if (useHttpMode) {
            httpStreamer.stop()
        } else {
            scope.cancel()
            try { 
                socket?.close(1000, "bye") 
            } catch (_: Throwable) {}
            socket = null
            connected = false
        }
        buffer.clearAll()
        windowLocalIndex.set(0)
        paused = false
    }

    /** 캡처 콜백에서 호출 */
    fun addFrame(pose: List<LM>, left: List<LM>, right: List<LM>) {
        if (paused) return
        if (pose.size != 23 || left.size != 21 || right.size != 21) return
        
        if (useHttpMode) {
            httpStreamer.addFrame(pose, left, right)
        } else {
            val idx = windowLocalIndex.getAndIncrement()
            buffer.add(FrameEntry(idx, pose, left, right))
        }
    }

    /** PAUSE/RESUME 액션을 서버로 알림 (frames 비움) */
    fun sendPauseResume(pausedNow: Boolean) {
        paused = pausedNow
        windowLocalIndex.set(0)
        
        if (useHttpMode) {
            httpStreamer.sendPauseResume(pausedNow)
        } else {
            // HTTP 명세에 맞는 형식으로 변경
            val t = playerPositionProvider?.invoke() ?: 0L
            val action = if (paused) "PAUSE" else "RESUME"
            val request = SimilarityRequest(
                type = action,
                timestamp = t,
                frames = emptyList()
            )
            val text = json.encodeToString(SimilarityRequest.serializer(), request)
            socket?.send(text)
        }
    }
    
    /** HTTP/WebSocket 모드 전환 */
    fun setHttpMode(enabled: Boolean) {
        useHttpMode = enabled
        android.util.Log.d("WebSocketStreamer", "HTTP 모드: $enabled")
    }
    
    private fun buildSimilarityRequest(type: String, timestamp: Long, frames: List<FrameEntry>): SimilarityRequest {
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
