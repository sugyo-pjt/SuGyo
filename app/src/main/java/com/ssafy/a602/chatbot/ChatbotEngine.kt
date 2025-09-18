package com.ssafy.a602.chatbot

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/* ───────────────── 도메인 모델/계약 ───────────────── */

enum class BotMode { LEARN, FREE }

data class Scenario(
    val id: String,
    val title: String,
    val openingBotLine: String,
    val firstBotQuestion: String
)

data class ChatMessage(
    val isBot: Boolean,
    val text: String
)

data class Pt(val x: Float, val y: Float)            // 0..1 정규화 좌표
data class HandFrame(val landmarks: List<Pt>)        // 21 포인트 가정

data class SessionInfo(val sessionId: String)
data class BotTurn(
    val botText: String,
    val frames: Flow<HandFrame>
)

/** 백엔드 계약(실서버/가짜서버 모두 이 인터페이스만 지키면 UI는 그대로 동작) */
interface ChatBackend {
    suspend fun startSession(scenarioId: String): SessionInfo
    suspend fun sendUserText(sessionId: String, text: String): BotTurn
}

/* ───────────────── 가짜 백엔드(로컬 데모용) ───────────────── */

class ChatFakeApi : ChatBackend {
    override suspend fun startSession(scenarioId: String): SessionInfo {
        delay(150) // 네트워크 흉내
        return SessionInfo(sessionId = "sess-${Random.nextInt(1000, 9999)}")
    }

    override suspend fun sendUserText(sessionId: String, text: String): BotTurn {
        // 1) 간단한 봇 텍스트
        val botText = when {
            text.contains("안녕") -> "안녕하세요! 반가워요."
            text.length <= 2      -> "좋아요. 좀 더 자세히 말해볼까요?"
            else                  -> "네, 이해했어요. 동작을 이어서 보여드릴게요."
        }

        // 2) 랜드마크 스트림(프레임) — 21개 점이 원을 그리듯 움직이는 더미
        val frames: Flow<HandFrame> = flow {
            val count = 60 // 60프레임 정도
            repeat(count) { t ->
                val angleBase = (t / 15f) * PI.toFloat()
                val pts = buildList {
                    val cx = 0.5f
                    val cy = 0.5f
                    val r = 0.25f
                    // 21 포인트 흉내
                    for (i in 0 until 21) {
                        val a = angleBase + i * (PI.toFloat() / 10f)
                        add(Pt(cx + r * cos(a), cy + r * sin(a)))
                    }
                }
                emit(HandFrame(pts))
                delay(33) // ~30fps
            }
        }

        return BotTurn(botText, frames)
    }
}

/* ───────────────── ViewModel(화면 로직/상태) ───────────────── */

class ChatbotViewModel(
    private val backend: ChatBackend
) : ViewModel() {

    // 공개 시나리오(하드코딩; 필요시 서버 제공으로 교체)
    val scenarios: List<Scenario> = listOf(
        Scenario("trip",  "✈ 여행",   "여행 시나리오를 시작해볼게요.", "어디로 떠날 계획인가요?"),
        Scenario("intro", "🙂 자기소개", "자기소개 시나리오를 시작할게요.", "이름이 뭐예요?"),
        Scenario("food",  "🍜 음식주문", "음식 주문을 연습해봐요.",     "무엇을 주문하고 싶나요?")
    )

    // 화면 상태들(Compose가 관찰)
    val mode = mutableStateOf(BotMode.LEARN)
    val selectedScenario = mutableStateOf<Scenario?>(null)
    val messages: SnapshotStateList<ChatMessage> = mutableStateListOf()

    val receiving = mutableStateOf(false)
    val currentFrame = mutableStateOf<HandFrame?>(null)
    val sessionId = mutableStateOf<String?>(null)

    private var streamJob: Job? = null

    init {
        // 초기 인사
        messages += ChatMessage(true, "안녕하세요! 수어 학습을 시작해볼까요?")
    }

    fun changeMode(newMode: BotMode) {
        if (mode.value == newMode) return
        mode.value = newMode
        // 상태 초기화
        streamJob?.cancel(); streamJob = null
        receiving.value = false
        currentFrame.value = null
        sessionId.value = null
        selectedScenario.value = null
        messages.clear()
        if (newMode == BotMode.LEARN) {
            messages += ChatMessage(true, "안녕하세요! 수어 학습을 시작해볼까요?")
        } else {
            messages += ChatMessage(true, "자유 모드예요. 수어(또는 텍스트)로 말해 주세요.")
        }
    }

    fun selectScenario(sc: Scenario) {
        selectedScenario.value = sc
        // 메시지 리셋 + 오프닝
        messages.clear()
        messages += ChatMessage(true, sc.openingBotLine)
        messages += ChatMessage(true, sc.firstBotQuestion)

        // 세션 생성
        viewModelScope.launch {
            val sess = backend.startSession(sc.id)
            sessionId.value = sess.sessionId
        }
    }

    fun sendUserText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        // 세션 없는 경우: LEARN은 선택 시 생성되지만,
        // FREE 모드에서는 자동으로 "free" 시나리오로 세션 생성
        if (sessionId.value == null) {
            viewModelScope.launch {
                val sid = backend.startSession(selectedScenario.value?.id ?: "free").sessionId
                sessionId.value = sid
                // 세션 생겼으니 다시 전송
                sendUserText(trimmed)
            }
            return
        }

        val sid = sessionId.value!!
        messages += ChatMessage(isBot = false, text = trimmed)

        // 이전 스트림 중단
        streamJob?.cancel()
        receiving.value = true
        currentFrame.value = null

        streamJob = viewModelScope.launch {
            try {
                val turn = backend.sendUserText(sid, trimmed)
                // 봇 텍스트 먼저 출력
                messages += ChatMessage(true, turn.botText)
                // 좌표 프레임 수신
                turn.frames.collect { frame ->
                    currentFrame.value = frame
                }
            } catch (t: Throwable) {
                messages += ChatMessage(true, "좌표 수신 중 오류가 발생했어요. 네트워크를 확인해 주세요.")
            } finally {
                receiving.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        streamJob?.cancel()
    }

    /* ViewModel Factory (Compose에서 viewModel(factory=...) 로 생성용) */
    class Factory(
        private val backend: ChatBackend
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatbotViewModel(backend) as T
        }
    }
}
