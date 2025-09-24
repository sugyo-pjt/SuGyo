package com.ssafy.a602.game.play.net

import com.ssafy.a602.game.play.input.*
import com.ssafy.a602.game.play.dto.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import okio.ByteString
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketStreamer @Inject constructor() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var socket: WebSocket? = null
    @Volatile private var connected = false
    @Volatile private var paused = false

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
        
        if (socket != null) return
        
        val request = Request.Builder()
            .url(url)
            .build()
            
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                connected = true
                android.util.Log.d("WebSocketStreamer", "웹소켓 연결 성공")
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val result = json.decodeFromString<WebSocketJudgmentResult>(text)
                    onJudgmentReceived?.invoke(result)
                } catch (e: Exception) {
                    android.util.Log.e("WebSocketStreamer", "판정 결과 파싱 실패: $text", e)
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
                    val envelope = buildAllFramesEnvelope(
                        action = GameActionType.PLAY,
                        timestampMs = t,
                        entries = list.toList()
                    )
                    val payload = json.encodeToString(AllFramesEnvelope.serializer(), envelope)
                    socket?.send(payload)
                    list.clear()
                }
            }
        }
    }

    fun stop() {
        scope.cancel()
        try { 
            socket?.close(1000, "bye") 
        } catch (_: Throwable) {}
        socket = null
        connected = false
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

    /** PAUSE/RESUME 액션을 서버로 알림 (frames 비움) */
    fun sendPauseResume(pausedNow: Boolean) {
        paused = pausedNow
        windowLocalIndex.set(0)
        
        // 액션만 담긴 Envelope
        val t = playerPositionProvider?.invoke() ?: 0L
        val action = if (paused) GameActionType.PAUSE else GameActionType.RESUME
        val envelope = AllFramesEnvelope(
            allFrames = listOf(
                ActionFrames(
                    action = action, 
                    timestamp = t, 
                    frames = emptyList()
                )
            )
        )
        val text = json.encodeToString(AllFramesEnvelope.serializer(), envelope)
        socket?.send(text)
    }
}
