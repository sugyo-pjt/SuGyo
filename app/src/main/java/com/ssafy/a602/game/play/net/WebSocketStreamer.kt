package com.ssafy.a602.game.play.net

import com.ssafy.a602.game.play.input.*
import com.ssafy.a602.game.play.dto.*
import com.ssafy.a602.auth.TokenManager
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
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
    
    // OkHttpClient 재사용 - 강화된 설정
    private val client by lazy {
        OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .pingInterval(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()
    }
    
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = false
    }
    
    // 판정 결과 수신 콜백
    private var onJudgmentReceived: ((WebSocketJudgmentResult) -> Unit)? = null
    
    /** 웹소켓 URL 스킴 보정 */
    private fun normalizeWsUrl(raw: String): String {
        // 이미 ws/wss면 그대로, http/https면 ws/wss로 변환
        return when {
            raw.startsWith("wss://") || raw.startsWith("ws://") -> raw
            raw.startsWith("https://") -> "wss://" + raw.removePrefix("https://")
            raw.startsWith("http://")  -> "ws://"  + raw.removePrefix("http://")
            else -> "wss://$raw" // 스킴 누락 시 기본 wss
        }
    }

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
        
        // URL 스킴 보정
        val fixed = normalizeWsUrl(url)
        android.util.Log.i("WebSocketStreamer", "CONNECT → $fixed")
        
        val request = Request.Builder()
            .url(fixed)
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
                    
                    // TODO: 서버에서 받는 데이터 구조가 확정되면 파싱 로직 구현
                    // 현재는 로그만 출력하고 나중에 실제 데이터 처리 로직 추가 예정
                    
                } catch (e: Exception) {
                    android.util.Log.e("WebSocketStreamer", "웹소켓 메시지 처리 실패: $text", e)
                }
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                connected = false
                socket = null
                android.util.Log.e("WebSocketStreamer", "FAIL url=${response?.request?.url} code=${response?.code} msg=${response?.message}", t)
                try {
                    android.util.Log.e("WebSocketStreamer", "HEADERS=${response?.headers}")
                    android.util.Log.e("WebSocketStreamer", "BODY=${response?.peekBody(Long.MAX_VALUE)?.string()}")
                } catch (_: Throwable) {}
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
        android.util.Log.d("WebSocketStreamer", "🚀 startStreaming 호출: useHttpMode=$useHttpMode, connected=$connected")
        
        if (useHttpMode) {
            android.util.Log.d("WebSocketStreamer", "🌐 HTTP 모드: HttpStreamer가 처리")
            return
        }
        
        scope.launch {
            android.util.Log.d("WebSocketStreamer", "🔄 전송 루프 시작")
            while (isActive) {
                delay(300L)
                if (!connected) { 
                    android.util.Log.v("WebSocketStreamer", "⏸️ 연결되지 않음 - 버퍼 클리어")
                    buffer.swapAndGet().clear()
                    windowLocalIndex.set(0)
                    continue 
                }
                if (paused) { 
                    android.util.Log.v("WebSocketStreamer", "⏸️ 일시정지 상태 - 버퍼 클리어")
                    buffer.swapAndGet().clear()
                    windowLocalIndex.set(0)
                    continue 
                }

                val list = buffer.swapAndGet()
                android.util.Log.v("WebSocketStreamer", "🔄 전송 루프: bufferSize=${list.size}, connected=$connected, paused=$paused")
                // 다음 윈도우부터 인덱스 0부터
                windowLocalIndex.set(0)

                if (list.isNotEmpty()) {
                    // ExoPlayer는 메인 스레드에서만 접근 가능하므로 withContext 사용
                    val t = withContext(Dispatchers.Main) {
                        playerPositionProvider?.invoke() ?: 0L
                    }
                    val request = buildSimilarityRequest("PLAY", t, list.toList())
                    val payload = json.encodeToString(SimilarityRequest.serializer(), request)
                    
                    android.util.Log.d("WebSocketStreamer", "📤 웹소켓 데이터 전송 시도: frames=${list.size}, timestamp=$t")
                    val success = socket?.send(payload) ?: false
                    if (success) {
                        android.util.Log.d("WebSocketStreamer", "✅ 웹소켓 데이터 전송 성공")
                    } else {
                        android.util.Log.e("WebSocketStreamer", "❌ 웹소켓 데이터 전송 실패")
                    }
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
    fun addFrame(pose: List<LM?>, left: List<LM?>, right: List<LM?>) {
        android.util.Log.d("WebSocketStreamer", "📥 addFrame 호출: pose=${pose.size}, left=${left.size}, right=${right.size}, paused=$paused, useHttpMode=$useHttpMode")
        
        if (paused) {
            android.util.Log.d("WebSocketStreamer", "⏸️ 일시정지 상태로 인해 프레임 무시")
            return
        }
        // 크기 검사 (null 값 포함하여 정확한 크기 확인)
        if (pose.size != 23 || left.size != 21 || right.size != 21) {
            android.util.Log.w("WebSocketStreamer", "⚠️ 프레임 크기 불일치: pose=${pose.size}, left=${left.size}, right=${right.size}")
            return
        }
        
        android.util.Log.d("WebSocketStreamer", "📊 프레임 데이터: pose=${pose.size}, left=${left.size}, right=${right.size}")
        android.util.Log.d("WebSocketStreamer", "📊 null 값 개수: pose=${pose.count { it == null }}, left=${left.count { it == null }}, right=${right.count { it == null }}")
        
        if (useHttpMode) {
            android.util.Log.d("WebSocketStreamer", "🌐 HTTP 모드: HttpStreamer로 전달")
            httpStreamer.addFrame(pose, left, right)
        } else {
            val idx = windowLocalIndex.getAndIncrement()
            buffer.add(FrameEntry(idx, pose, left, right))
            android.util.Log.d("WebSocketStreamer", "📦 프레임 버퍼에 추가: idx=$idx")
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
            // ExoPlayer는 메인 스레드에서만 접근 가능하므로 withContext 사용
            scope.launch(Dispatchers.Main) {
                val t = playerPositionProvider?.invoke() ?: 0L
                val action = if (paused) "PAUSE" else "RESUME"
                val request = SimilarityRequest(
                    type = action,
                    timestamp = t,
                    frames = emptyList()
                )
                val text = json.encodeToString(SimilarityRequest.serializer(), request)
                
                android.util.Log.d("WebSocketStreamer", "📤 웹소켓 $action 전송 시도: timestamp=$t")
                val success = socket?.send(text) ?: false
                if (success) {
                    android.util.Log.d("WebSocketStreamer", "✅ 웹소켓 $action 전송 성공")
                } else {
                    android.util.Log.e("WebSocketStreamer", "❌ 웹소켓 $action 전송 실패")
                }
            }
        }
    }
    
    /** HTTP/WebSocket 모드 전환 */
    fun setHttpMode(enabled: Boolean) {
        useHttpMode = enabled
        android.util.Log.d("WebSocketStreamer", "HTTP 모드: $enabled")
    }
    
    /** 여러 URL 후보를 순차적으로 시도하는 연결 메서드 */
    /*
    fun connectWithFallback(
        urls: List<String>,
        playerPositionMs: () -> Long,
        onJudgment: (WebSocketJudgmentResult) -> Unit
    ) {
        playerPositionProvider = playerPositionMs
        this.onJudgmentReceived = onJudgment
        
        if (useHttpMode) {
            // HTTP 모드 사용
            httpStreamer.startStreaming(playerPositionMs, onJudgment)
            return
        }
        
        // 토큰 가져오기
        val token = tokenManager.getAccessToken()
        if (token.isNullOrEmpty()) {
            android.util.Log.e("WebSocketStreamer", "❌ no token")
            setHttpMode(true)
            httpStreamer.startStreaming(playerPositionMs, onJudgment)
            return
        }
        
        // 지수 백오프 재시도 로직
        scope.launch {
            reconnectWithBackback(urls, token, onJudgment)
        }
    }
    */
    
    /** 지수 백오프 재시도 로직 */
    /*
    private suspend fun reconnectWithBackback(
        urls: List<String>,
        token: String,
        onJudgment: (WebSocketJudgmentResult) -> Unit
    ) {
        var delayMs = 300L
        repeat(3) { attempt ->
            android.util.Log.i("WebSocketStreamer", "재시도 $attempt: delay=${delayMs}ms")
            
            for (url in urls) {
                val fixed = normalizeWsUrl(url)
                android.util.Log.i("WebSocketStreamer", "try $fixed")
                val req = buildWsRequest(fixed, token, useOrigin = false, useSubproto = false)
                connectOnce(req, onJudgment)
                
                // 연결 성공 대기 (최대 1초)
                var waitTime = 0L
                while (!connected && waitTime < 1000L) {
                    kotlinx.coroutines.delay(50)
                    waitTime += 50
                }
                
                if (connected) {
                    android.util.Log.d("WebSocketStreamer", "✅ 웹소켓 연결 성공: $fixed")
                    return
                }
                
                android.util.Log.w("WebSocketStreamer", "연결 실패, 다음 URL 시도")
            }
            
            if (attempt < 2) { // 마지막 시도가 아니면 대기
                kotlinx.coroutines.delay(delayMs)
                delayMs *= 2
            }
        }
        
        android.util.Log.e("WebSocketStreamer", "❌ WS all retries failed → switch to HTTP mode")
        setHttpMode(true)
        httpStreamer.startStreaming(playerPositionProvider ?: { 0L }, onJudgment)
    }
    */
    
    /** 단일 연결 시도 */
    /*
    private fun connectOnce(request: Request, onJudgment: (WebSocketJudgmentResult) -> Unit): Boolean {
        if (socket != null) return false
        
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                connected = true
                android.util.Log.d("WebSocketStreamer", "웹소켓 연결 성공: ${request.url}")
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    android.util.Log.d("WebSocketStreamer", "웹소켓 메시지 수신: $text")
                    
                    // TODO: 서버에서 받는 데이터 구조가 확정되면 파싱 로직 구현
                    // 현재는 로그만 출력하고 나중에 실제 데이터 처리 로직 추가 예정
                    
                } catch (e: Exception) {
                    android.util.Log.e("WebSocketStreamer", "웹소켓 메시지 처리 실패: $text", e)
                }
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                connected = false
                socket = null
                android.util.Log.e("WebSocketStreamer", "FAIL url=${response?.request?.url} code=${response?.code} msg=${response?.message}", t)
                try {
                    android.util.Log.e("WebSocketStreamer", "HEADERS=${response?.headers}")
                    android.util.Log.e("WebSocketStreamer", "BODY=${response?.peekBody(Long.MAX_VALUE)?.string()}")
                } catch (_: Throwable) {}
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                connected = false
                socket = null
                android.util.Log.d("WebSocketStreamer", "웹소켓 연결 종료: $code - $reason")
            }
        })
        
        // 연결 시도 후 즉시 true 반환 (실제 성공 여부는 onOpen/onFailure에서 처리)
        return true
    }
    */
    
    /** 웹소켓 요청 빌더 */
    /*
    private fun buildWsRequest(url: String, token: String, useOrigin: Boolean = false, useSubproto: Boolean = false): Request {
        val builder = Request.Builder().url(url)
            .addHeader("Authorization", "Bearer $token")
        
        if (useOrigin) {
            builder.addHeader("Origin", "https://j13a602.p.ssafy.io")
        }
        if (useSubproto) {
            builder.addHeader("Sec-WebSocket-Protocol", "json")
        }
        
        return builder.build()
    }
    */
    
    private fun buildSimilarityRequest(type: String, timestamp: Long, frames: List<FrameEntry>): SimilarityRequest {
        val frameBlocks = frames.mapIndexed { index, frameEntry ->
            FrameBlock(
                frame = index + 1,
                poses = listOf(
                    PoseBlock(
                        part = "BODY",
                        coordinates = frameEntry.pose.mapNotNull { lm ->
                            lm?.let {
                                Coordinate(
                                    x = it.x,
                                    y = it.y,
                                    z = it.z,
                                    w = it.w
                                )
                            }
                        }
                    ),
                    PoseBlock(
                        part = "LEFT_HAND",
                        coordinates = frameEntry.left.mapNotNull { lm ->
                            lm?.let {
                                Coordinate(
                                    x = it.x,
                                    y = it.y,
                                    z = it.z,
                                    w = it.w
                                )
                            }
                        }
                    ),
                    PoseBlock(
                        part = "RIGHT_HAND",
                        coordinates = frameEntry.right.mapNotNull { lm ->
                            lm?.let {
                                Coordinate(
                                    x = it.x,
                                    y = it.y,
                                    z = it.z,
                                    w = it.w
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
