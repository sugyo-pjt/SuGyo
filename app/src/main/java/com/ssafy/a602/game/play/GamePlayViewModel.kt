package com.ssafy.a602.game.play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a602.game.data.GameDataManager
import com.ssafy.a602.game.data.GameMode
import com.ssafy.a602.game.play.input.LM
import com.ssafy.a602.game.play.dto.WebSocketJudgmentResult
import com.ssafy.a602.game.play.net.WebSocketStreamer
import com.ssafy.a602.game.score.GameScoreCalculator
import com.ssafy.a602.game.score.JudgmentType as ScoreJudgmentType
import com.ssafy.a602.game.play.JudgmentType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GameUiState(
    val loading: Boolean = false,
    val score: Int = 0,
    val combo: Int = 0,
    val maxCombo: Int = 0,
    val percent: Int = 0,
    val grade: String = "",
    val error: String? = null,
    val submitted: Boolean = false,
    val personalBest: Boolean = false,
    // GameScoreCalculator에서 가져온 정확한 데이터
    val correctCount: Int = 0,
    val missCount: Int = 0,
    val missWords: List<String> = emptyList(),
    // 일시정지 상태 추가
    val isPaused: Boolean = false
)

data class CompleteUiState(
    val submitting: Boolean = false,
    val submitError: String? = null,
    val submitted: Boolean = false,
    val isBestRecord: Boolean = false
)

@HiltViewModel
class GamePlayViewModel @Inject constructor(
    private val webSocketStreamer: WebSocketStreamer
) : ViewModel() {

    private lateinit var calc: GameScoreCalculator
    private var songId: String = ""
    private var currentMusicId: Long = -1L
    private var gameMode: GameMode = GameMode.EASY
    
    private val _ui = MutableStateFlow(GameUiState())
    val ui = _ui.asStateFlow()
    
    private val _complete = MutableStateFlow(CompleteUiState())
    val complete = _complete.asStateFlow()
    
    // 웹소켓 판정 결과 상태
    private val _currentJudgment = MutableStateFlow<JudgmentResult?>(null)
    val currentJudgment = _currentJudgment.asStateFlow()
    
    // 플레이어 위치 제공자 (ExoPlayer에서 가져올 수 있도록)
    private var playerPositionProvider: (() -> Long)? = null

    fun startGame(songId: String, totalWords: Int, mode: GameMode, playerPositionMs: () -> Long = { 0L }) {
        this.songId = songId
        this.gameMode = mode
        this.currentMusicId = songId.toLongOrNull() ?: -1L
        this.playerPositionProvider = playerPositionMs
        
        // Easy 모드일 때만 프론트엔드 계산기 사용
        if (mode == GameMode.EASY) {
            calc = GameScoreCalculator(songId = songId, totalWords = totalWords, baseScore = 100)
        }
        
        _ui.value = GameUiState()
        _complete.value = CompleteUiState()
        
        // 하드 모드일 때만 웹소켓 연결
        if (mode == GameMode.HARD) {
            connectWebSocket()
        }
    }
    
    private fun connectWebSocket() {
        val wsUrl = "wss://j13a602.p.ssafy.io/ws/game/rhythm"
        webSocketStreamer.connect(wsUrl, playerPositionProvider ?: { 0L }) { judgment ->
            // 웹소켓에서 받은 판정 결과를 기존 JudgmentResult로 변환
            val judgmentResult = JudgmentResult(
                type = when (judgment.judgment) {
                    "PERFECT" -> JudgmentType.PERFECT
                    "GREAT" -> JudgmentType.GREAT
                    "GOOD" -> JudgmentType.GOOD
                    "MISS" -> JudgmentType.MISS
                    else -> JudgmentType.MISS
                },
                accuracy = judgment.accuracy ?: when (judgment.judgment) {
                    "PERFECT" -> 0.98f
                    "GREAT" -> 0.85f
                    "GOOD" -> 0.70f
                    "MISS" -> 0.0f
                    else -> 0.0f
                },
                score = judgment.score,
                combo = judgment.combo,
                timestamp = System.currentTimeMillis(),
                word = judgment.word,
                isWebSocketResult = true
            )
            
            // UI에 판정 결과 표시
            _currentJudgment.value = judgmentResult
            
            // 🔥 Hard 모드: 서버에서 계산된 모든 결과를 그대로 사용
            _ui.value = _ui.value.copy(
                score = judgment.totalScore ?: _ui.value.score,
                combo = judgment.combo,
                maxCombo = judgment.maxCombo ?: _ui.value.maxCombo,
                percent = ((judgment.accuracy ?: 0f) * 100).toInt(),
                grade = judgment.grade ?: _ui.value.grade
            )
            
            // 3초 후 판정 결과 숨기기
            viewModelScope.launch {
                kotlinx.coroutines.delay(3000)
                _currentJudgment.value = null
            }
        }
        
        webSocketStreamer.startStreaming()
    }

    // 🔥 Easy 모드: 프론트엔드에서 계산
    fun onServerVerdict(isPerfect: Boolean, word: String) {
        // Easy 모드일 때만 프론트엔드 계산기 사용
        if (gameMode == GameMode.EASY) {
            val type = if (isPerfect) ScoreJudgmentType.PERFECT else ScoreJudgmentType.MISS
            calc.addJudgment(type, word)

            // HUD용 간단 요약만 즉시 갱신
            val preview = calc.getFinal()
            _ui.value = _ui.value.copy(
                score = preview.totalScore,
                percent = preview.percent,
                grade = preview.grade,
                maxCombo = preview.maxCombo,
                correctCount = preview.correctCount,
                missCount = preview.missCount,
                missWords = preview.missWords
            )
        }
        // Hard 모드일 때는 서버에서 계산된 결과를 사용하므로 여기서는 아무것도 하지 않음
    }
    
    // MediaPipe 결과를 웹소켓으로 전송 (하드 모드일 때만)
    fun onLandmarks(pose: List<LM>, left: List<LM>, right: List<LM>) {
        if (gameMode == GameMode.HARD) {
            webSocketStreamer.addFrame(pose, left, right)
        }
    }
    
    fun togglePause() {
        val currentPaused = _ui.value.isPaused
        _ui.value = _ui.value.copy(isPaused = !currentPaused)
        
        // 하드 모드일 때 웹소켓에 일시정지 상태 전송
        if (gameMode == GameMode.HARD) {
            webSocketStreamer.sendPauseResume(!currentPaused)
        }
    }
    
    // 🔥 게임 완료 처리도 모드별로 다르게
    fun finishGame() {
        if (gameMode == GameMode.EASY) {
            // Easy 모드: 프론트엔드 계산 결과 사용
            val req = calc.getFinal()
            viewModelScope.launch {
                _ui.value = _ui.value.copy(loading = true, error = null)
                val result = GameDataManager.submitGameResult(req)
                _ui.value = result.fold(
                    onSuccess = { response ->
                        _ui.value.copy(
                            loading = false,
                            submitted = true,
                            personalBest = response.isPersonalBest
                        )
                    },
                    onFailure = { e ->
                        _ui.value.copy(
                            loading = false,
                            error = (e.message ?: "결과 전송 실패")
                        )
                    }
                )
            }
        } else {
            // Hard 모드: 서버에서 계산된 결과를 사용 (웹소켓으로 받은 최종 결과)
            // 서버에서 게임 완료 신호를 받으면 자동으로 처리됨
        }
    }

    fun finishGameAndPost() {
        if (_complete.value.submitting) return // 더블탭 방지
        
        if (gameMode == GameMode.EASY) {
            // Easy 모드: 프론트엔드 계산 결과 사용
            val final = calc.getFinal() // 여기서 totalScore만 사용
            viewModelScope.launch {
                _complete.value = _complete.value.copy(submitting = true, submitError = null)
                val result = GameDataManager.completeGame(currentMusicId, final.totalScore)
                _complete.value = result.fold(
                    onSuccess = { response ->
                        _complete.value.copy(
                            submitting = false,
                            submitted = true,
                            isBestRecord = response.isBestRecord
                        )
                    },
                    onFailure = { e ->
                        // API 호출 실패해도 게임 결과 화면으로 넘어가도록 submitted = true로 설정
                        _complete.value.copy(
                            submitting = false,
                            submitted = true, // 실패해도 결과 화면으로 이동
                            submitError = e.message ?: "전송 실패"
                        )
                    }
                )
            }
        } else {
            // Hard 모드: 서버에서 계산된 결과를 사용 (웹소켓으로 받은 최종 결과)
            // 서버에서 게임 완료 신호를 받으면 자동으로 처리됨
            _complete.value = _complete.value.copy(submitted = true)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        if (gameMode == GameMode.HARD) {
            webSocketStreamer.stop()
        }
    }
    
}
