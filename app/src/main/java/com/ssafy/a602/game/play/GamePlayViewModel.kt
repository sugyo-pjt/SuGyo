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
import com.ssafy.a602.game.play.collector.RhythmCollector
import com.ssafy.a602.game.play.collector.MediaPipeToRhythmConverter
import com.ssafy.a602.game.play.dto.*
import com.ssafy.a602.game.play.service.RhythmUploadService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
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
    private val webSocketStreamer: WebSocketStreamer,
    private val rhythmUploadService: RhythmUploadService
) : ViewModel() {

    private lateinit var calc: GameScoreCalculator
    
    // 중복 호출 방지 플래그
    private var isUploading = false
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
    
    // 🔥 하드 모드 리듬 수집기
    private var rhythmCollector: RhythmCollector? = null

    fun startGame(songId: String, totalWords: Int, mode: GameMode, playerPositionMs: () -> Long = { 0L }) {
        android.util.Log.d("GamePlayViewModel", "🎮 게임 시작: songId=$songId, mode=$mode, totalWords=$totalWords")
        this.songId = songId
        this.gameMode = mode
        this.currentMusicId = songId.toLongOrNull() ?: -1L
        this.playerPositionProvider = playerPositionMs
        
        // Easy 모드일 때만 프론트엔드 계산기 사용
        if (mode == GameMode.EASY) {
            android.util.Log.d("GamePlayViewModel", "📊 Easy 모드: 프론트엔드 계산기 초기화")
            calc = GameScoreCalculator(songId = songId, totalWords = totalWords, baseScore = 100)
        }
        
        // 🔥 하드 모드일 때 리듬 수집기 초기화 (웹소켓 연결)
        if (mode == GameMode.HARD) {
            android.util.Log.d("GamePlayViewModel", "🔥 Hard 모드: 리듬 수집기 초기화 시작")
            rhythmCollector = RhythmCollector(
                musicId = currentMusicId.toInt(),
                coroutineScope = viewModelScope
            )
            rhythmCollector?.startCollection()
            android.util.Log.d("GamePlayViewModel", "🔥 Hard 모드: 리듬 수집기 초기화 완료")
            
            // 🔥 GameDataManager에 RhythmCollector 저장
            GameDataManager.setRhythmCollector(rhythmCollector)
            
            // 🔥 게임 시작 시 PLAY 세그먼트 시작
            viewModelScope.launch {
                rhythmCollector?.onTypeChanged("PLAY", 0L)
                android.util.Log.d("GamePlayViewModel", "🔥 Hard 모드: PLAY 세그먼트 시작")
            }
            
            // 하드 모드일 때만 웹소켓 연결
            connectWebSocket()
        }
        
        // 🎵 채보만들기 모드일 때 리듬 수집기 초기화 (웹소켓 연결 없음)
        if (mode == GameMode.CHART_CREATION) {
            android.util.Log.d("GamePlayViewModel", "🎵 채보만들기 모드: 리듬 수집기 초기화 시작")
            rhythmCollector = RhythmCollector(
                musicId = currentMusicId.toInt(),
                coroutineScope = viewModelScope
            )
            rhythmCollector?.startCollection()
            android.util.Log.d("GamePlayViewModel", "🎵 채보만들기 모드: 리듬 수집기 초기화 완료")
            
            // GameDataManager에 RhythmCollector 저장
            GameDataManager.setRhythmCollector(rhythmCollector)
            
            // 게임 시작 시 PLAY 세그먼트 시작
            viewModelScope.launch {
                rhythmCollector?.onTypeChanged("PLAY", 0L)
                android.util.Log.d("GamePlayViewModel", "🎵 채보만들기 모드: PLAY 세그먼트 시작")
            }
            // 웹소켓 연결하지 않음 - HTTP로만 저장
        }
        
        _ui.value = GameUiState()
        _complete.value = CompleteUiState()
    }
    
    private fun connectWebSocket() {
        if (currentMusicId <= 0) {
            android.util.Log.e("GamePlayViewModel", "❌ invalid musicId=$currentMusicId (songId=$songId)")
            webSocketStreamer.setHttpMode(true)
            return
        }
        tryWsWithCandidates(currentMusicId)
    }
    
    private fun tryWsWithCandidates(musicId: Long) {
        val base = "wss://j13a602.p.ssafy.io"
        val url = "$base/play/hard/$musicId"
        android.util.Log.d("GamePlayViewModel", "🔗 WebSocket 연결 시도: $url")
        webSocketStreamer.connect(url, playerPositionProvider ?: { 0L }) { judgment ->
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
        
        // 연결 성립 후에만 전송 시작
        android.util.Log.d("GamePlayViewModel", "🚀 startStreaming 호출 시작")
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
    fun onLandmarks(pose: List<LM?>, left: List<LM?>, right: List<LM?>) {
        android.util.Log.d("GamePlayViewModel", "🎯 onLandmarks 호출: gameMode=$gameMode, pose=${pose.size}, left=${left.size}, right=${right.size}")
        
        if (gameMode == GameMode.HARD) {
            android.util.Log.d("GamePlayViewModel", "🔥 Hard 모드: MediaPipe 데이터 수신 - pose=${pose.size}, left=${left.size}, right=${right.size}")
            
            // 데이터 유효성 검사
            if (pose.isEmpty() && left.isEmpty() && right.isEmpty()) {
                android.util.Log.w("GamePlayViewModel", "⚠️ 모든 MediaPipe 데이터가 비어있음!")
                return
            }
            
            webSocketStreamer.addFrame(pose, left, right)
            
            // 🔥 리듬 수집기에도 프레임 데이터 전달 (모든 프레임 즉시 수집)
            val poses = MediaPipeToRhythmConverter.convertToPoses(pose, left, right)
            // ExoPlayer는 메인 스레드에서만 접근 가능하므로 withContext 사용
            viewModelScope.launch {
                val positionMs = withContext(Dispatchers.Main) {
                    playerPositionProvider?.invoke() ?: 0L
                }
                android.util.Log.v("GamePlayViewModel", "🔥 Hard 모드: 리듬 수집기에 프레임 전달 - positionMs=$positionMs, poses=${poses.size}")
                rhythmCollector?.addFrameToBuffer(poses, positionMs)
            }
        }
    }
    
    fun togglePause() {
        val currentPaused = _ui.value.isPaused
        _ui.value = _ui.value.copy(isPaused = !currentPaused)
        
        // 하드 모드일 때 웹소켓에 일시정지 상태 전송
        if (gameMode == GameMode.HARD) {
            webSocketStreamer.sendPauseResume(!currentPaused)
            
            // 🔥 리듬 수집기에 세그먼트 타입 변경 알림
            val positionMs = playerPositionProvider?.invoke() ?: 0L
            val segmentType = if (currentPaused) "PAUSE" else "RESUME"
            viewModelScope.launch {
                rhythmCollector?.onTypeChanged(segmentType, positionMs)
            }
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
        android.util.Log.d("GamePlayViewModel", "🎯 게임 완료 처리 시작: mode=$gameMode")
        
        // 중복 호출 방지 강화
        if (_complete.value.submitting || isUploading || _complete.value.submitted) {
            android.util.Log.d("GamePlayViewModel", "🎯 이미 처리 중 또는 완료됨 - 중복 호출 방지")
            return
        }
        
        when (gameMode) {
            GameMode.EASY -> {
                android.util.Log.d("GamePlayViewModel", "📊 Easy 모드: 게임 완료 처리 (save API 호출 없음)")
                // Easy 모드: save API 호출 없이 게임 완료 처리
                _complete.value = _complete.value.copy(
                    submitting = false,
                    submitted = true,
                    isBestRecord = false
                )
            }
            
            GameMode.HARD -> {
                android.util.Log.d("GamePlayViewModel", "🔥 Hard 모드: 게임 완료 처리 (POST 요청은 결과화면에서 처리)")
                // 🔥 Hard 모드: POST 요청은 GameResultScreen에서 처리
                _complete.value = _complete.value.copy(
                    submitting = false,
                    submitted = true,
                    isBestRecord = false
                )
            }
            
            GameMode.CHART_CREATION -> {
                android.util.Log.d("GamePlayViewModel", "🎵 채보만들기 모드: 게임 완료 처리 (POST 요청은 결과화면에서 처리)")
                // 🎵 채보만들기 모드: POST 요청은 GameResultScreen에서 처리
                _complete.value = _complete.value.copy(
                    submitting = false,
                    submitted = true,
                    isBestRecord = false
                )
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        when (gameMode) {
            GameMode.HARD -> {
                // Hard 모드: 웹소켓 연결 해제
                webSocketStreamer.stop()
                rhythmCollector?.stopCollection()
            }
            GameMode.CHART_CREATION -> {
                // 채보만들기 모드: 리듬 수집기만 정리 (웹소켓 없음)
                rhythmCollector?.stopCollection()
            }
            else -> {
                // Easy 모드: 특별한 정리 작업 없음
            }
        }
    }
    
}
