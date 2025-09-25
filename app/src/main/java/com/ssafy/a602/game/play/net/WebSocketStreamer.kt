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
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    private var playerPositionProvider: (() -> Long)? = null
    
    // 하이브리드 구조: 핑퐁 버퍼 + 버킷 기반
    private val buffer = PingPongBuffer<FrameEntry>(initialCapacity = 10)
    private val windowLocalIndex = AtomicInteger(0)
    
    // 버킷 기반 구조
    private val bucketMap = mutableMapOf<Long, MutableList<FrameEntry>>()
    private var lastBucket: Long? = null
    private val bucketMutex = Mutex()
    private val sentBuckets = mutableSetOf<Long>() // 이미 전송된 버킷 추적
    
    // ExoPlayer 포지션 캐시 (스레드 안전)
    private val lastPlayerPos = AtomicLong(0L)
    
    // FPS 모니터링
    private var lastFrameTime: Long? = null
    
    // 송신 큐 역압 처리 (동시성 안전)
    private val sendQueue = mutableListOf<Pair<Long, List<FrameEntry>>>()
    private val maxQueueSize = 10 // 최대 대기열 크기
    private val sendMutex = Mutex() // 송신 큐 동시 접근 보호
    
    // 포지션 업데이트 핸들러 (메인 스레드 전용)
    private val positionHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val positionRunnable = object : Runnable {
        override fun run() {
            try {
                // ExoPlayer는 반드시 메인 스레드에서만 접근
                val position = playerPositionProvider?.invoke() ?: 0L
                lastPlayerPos.set(position)
                android.util.Log.v("WebSocketStreamer", "📍 포지션 업데이트: $position")
            } catch (e: Exception) {
                android.util.Log.e("WebSocketStreamer", "포지션 업데이트 실패", e)
            }
            positionHandler.postDelayed(this, 16L) // ~60Hz
        }
    }
    
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

    /** 버킷 기반 스트리밍 시작 */
    fun startStreaming() {
        android.util.Log.d("WebSocketStreamer", "🚀 startStreaming 호출: useHttpMode=$useHttpMode, connected=$connected")
        
        if (useHttpMode) {
            android.util.Log.d("WebSocketStreamer", "🌐 HTTP 모드: HttpStreamer가 처리")
            return
        }
        
        if (!connected) {
            android.util.Log.w("WebSocketStreamer", "⚠️ 웹소켓이 연결되지 않음 - 스트리밍 시작 불가")
            return
        }
        
        // 포지션 업데이트 시작
        positionHandler.post(positionRunnable)
        android.util.Log.d("WebSocketStreamer", "📍 포지션 업데이트 시작 (60Hz)")
        
        // 주기적 백업 전송 (핑퐁 버퍼 보호)
        scope.launch(Dispatchers.IO) {
            while (isActive && connected) {
                delay(300L) // 300ms마다 체크
                if (!paused) {
                    flushPingPongBuffer()
                }
            }
        }
    }

    fun stop() {
        if (useHttpMode) {
            httpStreamer.stop()
        } else {
            // 포지션 업데이트 중지
            positionHandler.removeCallbacks(positionRunnable)
            
            // 취소 전에 잔여 데이터 플러시 (runBlocking으로 동기 처리)
            runBlocking(Dispatchers.IO) {
                bucketMutex.withLock {
                    // 핑퐁 버퍼의 남은 프레임들 먼저 처리
                    flushPingPongBuffer()
                    
                    // 남은 모든 버킷 순차 전송
                    val remainingBuckets = bucketMap.keys.sorted()
                    android.util.Log.d("WebSocketStreamer", "🔄 잔여 버킷 플러시: ${remainingBuckets.size}개")
                    
                    remainingBuckets.forEach { bucket ->
                        flushBucket(bucket)
                    }
                    
                    bucketMap.clear()
                }
                
                // 송신 큐 완전히 비우기
                sendFromQueue()
                
                // 마지막 구간도 포함하여 END 이벤트 전송
                val finalBucket = lastBucket ?: 0L
                if (finalBucket >= 0) {
                    sendEndEvent(finalBucket)
                }
                
                // 자료구조 정리
                sendMutex.withLock {
                    sentBuckets.clear()
                    sendQueue.clear()
                }
                lastBucket = null
            }
            
            // 이제 scope 취소
            scope.cancel()
            try { 
                socket?.close(1000, "bye") 
            } catch (_: Throwable) {}
            socket = null
            connected = false
        }
        
        // 핑퐁 버퍼 정리
        buffer.clearAll()
        windowLocalIndex.set(0)
        paused = false
    }

    /** 캡처 콜백에서 호출 - 버킷 기반 처리 */
    fun addFrame(pose: List<LM?>, left: List<LM?>, right: List<LM?>) {
        android.util.Log.d("WebSocketStreamer", "📥 addFrame 호출: pose=${pose.size}, left=${left.size}, right=${right.size}, paused=$paused, useHttpMode=$useHttpMode")
        
        if (paused) {
            android.util.Log.d("WebSocketStreamer", "⏸️ 일시정지 상태로 인해 프레임 무시")
            return
        }
        
        if (!connected) {
            android.util.Log.v("WebSocketStreamer", "❌ 연결되지 않음 - 프레임 무시")
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
            return
        }
        
        // FrameEntry는 원본 LM 타입을 그대로 사용 (FPS 변동 대응)
        val frameIndex = windowLocalIndex.getAndIncrement()
        val frameEntry = FrameEntry(
            frameIndex = frameIndex,
            pose = pose,
            left = left,
            right = right
        )
        
        android.util.Log.v("WebSocketStreamer", "📊 프레임 수집: idx=$frameIndex, fps=${1000L / maxOf(1L, System.currentTimeMillis() - (lastFrameTime ?: System.currentTimeMillis()))}")
        lastFrameTime = System.currentTimeMillis()
        
        // 핑퐁 버퍼에 안전하게 저장
        buffer.add(frameEntry)
        android.util.Log.d("WebSocketStreamer", "📦 프레임 버퍼에 추가: idx=${frameEntry.frameIndex}")
        
        // 버킷 기반 처리 (백그라운드에서)
        scope.launch(Dispatchers.IO) {
            processFrameWithBucket(frameEntry)
        }
    }
    
    /** 버킷 기반 프레임 처리 */
    private suspend fun processFrameWithBucket(frameEntry: FrameEntry) {
        val currentPos = lastPlayerPos.get()
        val bucket = bucketOf(currentPos)
        
        android.util.Log.d("WebSocketStreamer", "🪣 프레임 버킷팅: pos=$currentPos, bucket=$bucket")
        
        bucketMutex.withLock {
            // 현재 버킷에 프레임 추가
            val bucketList = bucketMap.getOrPut(bucket) { mutableListOf() }
            bucketList.add(frameEntry)
            
            val prevBucket = lastBucket
            if (prevBucket == null) {
                // 첫 번째 프레임
                lastBucket = bucket
                android.util.Log.d("WebSocketStreamer", "🆕 첫 번째 버킷: $bucket")
                return@withLock
            }
            
            if (bucket > prevBucket) {
                // 버킷이 바뀜 - 백필 처리
                android.util.Log.d("WebSocketStreamer", "🔄 버킷 변경: $prevBucket → $bucket")
                flushBucketsBetween(prevBucket, bucket)
                lastBucket = bucket
            }
        }
    }
    
    /** 300ms 구간 계산 (경계값 처리) */
    private fun bucketOf(positionMs: Long): Long {
        return if (positionMs < 0) 0L else (positionMs / 300) * 300
    }
    
    /** 중간 버킷들 백필 처리 */
    private suspend fun flushBucketsBetween(startBucket: Long, endBucket: Long) {
        // 이전 버킷부터 현재 버킷 전까지 순차적으로 전송
        var currentBucket = startBucket
        while (currentBucket < endBucket) {
            flushBucket(currentBucket)
            currentBucket += 300
        }
    }
    
    /** 개별 버킷 전송 (동시성 안전) */
    private suspend fun flushBucket(bucket: Long) {
        // 이미 전송된 버킷은 건너뜀 (동시성 안전)
        val alreadySent = sendMutex.withLock { sentBuckets.contains(bucket) }
        if (alreadySent) {
            android.util.Log.d("WebSocketStreamer", "⏭️ 이미 전송된 버킷: $bucket")
            return
        }
        
        val frames = bucketMap.remove(bucket) ?: emptyList()
        
        android.util.Log.d("WebSocketStreamer", "📤 버킷 전송: bucket=$bucket, frames=${frames.size}")
        
        // 송신 큐 동시 접근 보호
        sendMutex.withLock {
            // 송신 큐 역압 처리
            if (sendQueue.size >= maxQueueSize) {
                android.util.Log.w("WebSocketStreamer", "⚠️ 송신 큐 포화: 가장 오래된 빈 버킷 드롭")
                // 가장 오래된 빈 버킷부터 드롭
                val dropIdx = sendQueue.indexOfFirst { it.second.isEmpty() }.let { if (it >= 0) it else 0 }
                sendQueue.removeAt(dropIdx)
            }
            
            // 큐에 추가
            sendQueue.add(bucket to frames)
        }
        
        // 비동기 전송
        scope.launch(Dispatchers.IO) {
            sendFromQueue()
        }
    }
    
    /** 핑퐁 버퍼 백업 전송 (데이터 유실 방지) */
    private suspend fun flushPingPongBuffer() {
        val frames = buffer.swapAndGet()
        if (frames.isNotEmpty()) {
            val currentPos = lastPlayerPos.get()
            val bucket = bucketOf(currentPos)
            
            android.util.Log.d("WebSocketStreamer", "🔄 핑퐁 버퍼 백업: frames=${frames.size}, bucket=$bucket")
            
            // 핑퐁 버퍼의 프레임들을 현재 버킷에 추가
            bucketMutex.withLock {
                val bucketList = bucketMap.getOrPut(bucket) { mutableListOf() }
                bucketList.addAll(frames)
            }
            
            // 윈도우 인덱스 리셋
            windowLocalIndex.set(0)
        }
    }
    
    /** 송신 큐에서 순차 전송 (동시성 안전) */
    private suspend fun sendFromQueue() {
        while (true) {
            val item = sendMutex.withLock { 
                if (sendQueue.isEmpty()) null else sendQueue.removeAt(0) 
            }
            item ?: break
            
            val (bucket, frames) = item
            
            try {
                val request = buildSimilarityRequest("PLAY", bucket, frames)
                val payload = json.encodeToString(SimilarityRequest.serializer(), request)
                
                android.util.Log.d("WebSocketStreamer", "📤 큐에서 전송: bucket=$bucket, frames=${frames.size}")
                val success = socket?.send(payload) ?: false
                
                if (success) {
                    android.util.Log.d("WebSocketStreamer", "✅ 큐 전송 성공: $bucket")
                    sendMutex.withLock { sentBuckets.add(bucket) } // 동일 락으로 일원화
                } else {
                    android.util.Log.e("WebSocketStreamer", "❌ 큐 전송 실패: $bucket")
                    // 실패 시 재시도 (큐 앞쪽에 다시 추가)
                    sendMutex.withLock { sendQueue.add(0, item) }
                    delay(100L) // 재시도 전 대기
                    break
                }
            } catch (e: Exception) {
                android.util.Log.e("WebSocketStreamer", "큐 전송 중 오류", e)
                break
            }
        }
    }
    
    /** 곡 종료 시 END 이벤트 전송 */
    private suspend fun sendEndEvent(finalBucket: Long) {
        try {
            val request = buildSimilarityRequest("END", finalBucket, emptyList())
            val payload = json.encodeToString(SimilarityRequest.serializer(), request)
            
            android.util.Log.d("WebSocketStreamer", "🏁 END 이벤트 전송: bucket=$finalBucket")
            val success = socket?.send(payload) ?: false
            if (success) {
                android.util.Log.d("WebSocketStreamer", "✅ END 이벤트 전송 성공")
            } else {
                android.util.Log.e("WebSocketStreamer", "❌ END 이벤트 전송 실패")
            }
        } catch (e: Exception) {
            android.util.Log.e("WebSocketStreamer", "END 이벤트 전송 중 오류", e)
        }
    }

    /** PAUSE/RESUME 액션을 서버로 알림 (재생 시간 기준) */
    fun sendPauseResume(pausedNow: Boolean) {
        paused = pausedNow
        
        if (useHttpMode) {
            httpStreamer.sendPauseResume(pausedNow)
        } else {
            // 재생 시간 기준으로 타임스탬프 계산 (정지 구간 제외)
            scope.launch(Dispatchers.Main) {
                val position = playerPositionProvider?.invoke() ?: 0L
                val bucket = bucketOf(position) // 300ms 단위 정규화
                val action = if (paused) "PAUSE" else "RESUME"
                
                val request = SimilarityRequest(
                    type = action,
                    timestamp = bucket,
                    frames = emptyList()
                )
                val text = json.encodeToString(SimilarityRequest.serializer(), request)
                
                android.util.Log.d("WebSocketStreamer", "📤 웹소켓 $action 전송 시도: timestamp=$bucket (재생시간 기준)")
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
                        coordinates = frameEntry.pose.map { lm ->
                            lm?.let { 
                                Coordinate(
                                    x = it.x,
                                    y = it.y,
                                    z = it.z,
                                    w = it.w
                                )
                            } ?: Coordinate(x = null, y = null, z = null, w = null)
                        }
                    ),
                    PoseBlock(
                        part = "LEFT_HAND",
                        coordinates = frameEntry.left.map { lm ->
                            lm?.let { 
                                Coordinate(
                                    x = it.x,
                                    y = it.y,
                                    z = it.z,
                                    w = it.w
                                )
                            } ?: Coordinate(x = null, y = null, z = null, w = null)
                        }
                    ),
                    PoseBlock(
                        part = "RIGHT_HAND",
                        coordinates = frameEntry.right.map { lm ->
                            lm?.let { 
                                Coordinate(
                                    x = it.x,
                                    y = it.y,
                                    z = it.z,
                                    w = it.w
                                )
                            } ?: Coordinate(x = null, y = null, z = null, w = null)
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
