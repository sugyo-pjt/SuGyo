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
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.ssafy.a602.game.play.dto.*
import com.ssafy.a602.game.play.service.RhythmUploadService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
// CameraX 관련 import 추가
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.QualitySelector
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.core.Preview
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import java.io.File
import android.util.Log
import kotlinx.coroutines.delay

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
    private val rhythmUploadService: RhythmUploadService,
    @ApplicationContext private val context: Context
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
    
    
    // 📹 CameraX 관련 필드 (ViewModel이 관리)
    private var videoCapture: VideoCapture<Recorder>? = null
    private var currentRecording: androidx.camera.video.Recording? = null
    private var isRecording = false
    
    // 📹 CameraX 상태 관리
    private val _cameraState = MutableStateFlow(CameraState())
    val cameraState = _cameraState.asStateFlow()
    
    data class CameraState(
        val isInitialized: Boolean = false,
        val isRecording: Boolean = false,
        val recordingPath: String? = null,
        val error: String? = null
    )

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
        
        // 🎵 채보만들기 모드일 때는 별도 초기화 필요
        // CameraX 바인딩은 Fragment/Activity에서 처리
        if (mode == GameMode.CHART_CREATION) {
            android.util.Log.d("GamePlayViewModel", "🎵 채보만들기 모드: CameraX 바인딩은 UI에서 처리")
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
                    "Perfect" -> JudgmentType.PERFECT
                    "Good" -> JudgmentType.GOOD
                    "Miss" -> JudgmentType.MISS
                    else -> JudgmentType.MISS
                },
                accuracy = when (judgment.judgment) {
                    "Perfect" -> 0.98f
                    "Good" -> 0.70f
                    "Miss" -> 0.0f
                    else -> 0.0f
                },
                score = judgment.points,
                combo = judgment.combo,
                timestamp = System.currentTimeMillis(),
                word = null, // 새로운 형식에서는 단어 정보 없음
                isWebSocketResult = true
            )
            
            // UI에 판정 결과 표시
            _currentJudgment.value = judgmentResult
            
            // 🔥 Hard 모드: 서버에서 계산된 모든 결과를 그대로 사용
            _ui.value = _ui.value.copy(
                score = judgment.totalScore,
                combo = judgment.combo,
                maxCombo = maxOf(_ui.value.maxCombo, judgment.combo),
                correctCount = judgment.perfectCount,
                missCount = judgment.missCount,
                percent = if (judgment.perfectCount + judgment.goodCount + judgment.missCount > 0) {
                    ((judgment.perfectCount + judgment.goodCount) * 100) / (judgment.perfectCount + judgment.goodCount + judgment.missCount)
                } else 0
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
                android.util.Log.d("GamePlayViewModel", "🎵 채보만들기 모드: 게임 완료 처리 (녹화 중지 후 일괄 MediaPipe 처리)")
                // 🎵 채보만들기 모드: 녹화 중지 후 일괄 처리
                stopCameraRecording()
                
                // 녹화 중지 후 파일 저장 완료를 위한 대기
                viewModelScope.launch {
                    delay(2000) // 2초 대기하여 녹화 파일이 완전히 저장되도록 함
                    android.util.Log.d("GamePlayViewModel", "🎵 채보만들기 모드: 녹화 파일 저장 완료 대기 완료")
                }
                
                _complete.value = _complete.value.copy(
                    submitting = false,
                    submitted = true,
                    isBestRecord = false
                )
            }
        }
    }
    
    // 📹 CameraX 관련 메서드들 (ViewModel Scope에서 안전하게 실행)
    
    /**
     * VideoCapture 인스턴스를 ViewModel에 설정
     */
    fun setVideoCaptureInstance(capture: VideoCapture<Recorder>) {
        Log.d("GamePlayViewModel", "📹 VideoCapture 인스턴스 설정")
        videoCapture = capture
        _cameraState.value = _cameraState.value.copy(isInitialized = true)
    }
    
    /**
     * CameraX 녹화 시작 (ViewModel Scope에서 안전하게 실행)
     */
    fun startCameraRecording(outputFile: File) {
        if (isRecording) {
            Log.w("GamePlayViewModel", "📹 이미 녹화 중입니다")
            return
        }
        
        val capture = videoCapture ?: run {
            Log.e("GamePlayViewModel", "📹 VideoCapture 인스턴스가 준비되지 않았습니다")
            _cameraState.value = _cameraState.value.copy(error = "VideoCapture가 초기화되지 않았습니다")
            return
        }
        
        Log.d("GamePlayViewModel", "📹 CameraX 녹화 시작: ${outputFile.absolutePath}")
        
        try {
            currentRecording = capture.output
                .prepareRecording(context, FileOutputOptions.Builder(outputFile).build())
                .start(ContextCompat.getMainExecutor(context)) { event ->
                    when (event) {
                        is VideoRecordEvent.Start -> {
                            Log.d("GamePlayViewModel", "📹 ✅ 녹화 시작됨")
                            isRecording = true
                            _cameraState.value = _cameraState.value.copy(
                                isRecording = true,
                                recordingPath = outputFile.absolutePath,
                                error = null
                            )
                        }
                        is VideoRecordEvent.Finalize -> {
                            Log.d("GamePlayViewModel", "📹 녹화 Finalize 이벤트")
                            if (event.hasError()) {
                                Log.e("GamePlayViewModel", "📹 ❌ 녹화 실패: ${event.error}")
                                _cameraState.value = _cameraState.value.copy(
                                    isRecording = false,
                                    error = "녹화 실패: ${event.error}"
                                )
                            } else {
                                Log.d("GamePlayViewModel", "📹 ✅ 녹화 완료: ${event.outputResults.outputUri}")
                                _cameraState.value = _cameraState.value.copy(
                                    isRecording = false,
                                    error = null
                                )
                            }
                            isRecording = false
                            currentRecording = null
                        }
                        is VideoRecordEvent.Status -> {
                            Log.d("GamePlayViewModel", "📹 녹화 상태: ${event}")
                        }
                        is VideoRecordEvent.Pause -> {
                            Log.d("GamePlayViewModel", "📹 녹화 일시정지")
                        }
                        is VideoRecordEvent.Resume -> {
                            Log.d("GamePlayViewModel", "📹 녹화 재개")
                        }
                    }
                }
            
            Log.d("GamePlayViewModel", "📹 CameraX 녹화 시작 요청 완료")
        } catch (e: Exception) {
            Log.e("GamePlayViewModel", "📹 CameraX 녹화 시작 실패", e)
            _cameraState.value = _cameraState.value.copy(
                isRecording = false,
                error = "녹화 시작 실패: ${e.message}"
            )
        }
    }
    
    /**
     * CameraX 녹화 중지
     */
    fun stopCameraRecording() {
        if (!isRecording) {
            Log.w("GamePlayViewModel", "📹 녹화 중이 아닙니다")
            return
        }
        
        Log.d("GamePlayViewModel", "📹 CameraX 녹화 중지")
        try {
            currentRecording?.stop()
        } catch (e: Exception) {
            Log.w("GamePlayViewModel", "📹 녹화 중지 중 오류 발생", e)
        } finally {
            currentRecording = null
            isRecording = false
            _cameraState.value = _cameraState.value.copy(isRecording = false)
        }
    }
    
    /**
     * CameraX 초기화 및 녹화 시작 (통합 메서드)
     */
    fun initializeCameraXAndStartRecording(
        context: Context,
        lifecycleOwner: androidx.lifecycle.LifecycleOwner,
        outputFile: File
    ) {
        Log.d("GamePlayViewModel", "📹 CameraX 초기화 및 녹화 시작")
        
        try {
            // 1. CameraX 초기화
            initializeCameraX(context, lifecycleOwner)
            
            // 2. 초기화 완료 후 녹화 시작
            viewModelScope.launch {
                delay(2000) // 2초 대기하여 카메라 초기화 완료 보장
                startCameraRecording(outputFile)
            }
        } catch (e: Exception) {
            Log.e("GamePlayViewModel", "📹 CameraX 초기화 실패", e)
            _cameraState.value = _cameraState.value.copy(error = "CameraX 초기화 실패: ${e.message}")
        }
    }
    
    /**
     * CameraX 초기화 (ViewModel에서 직접 관리)
     */
    private fun initializeCameraX(
        context: Context,
        lifecycleOwner: androidx.lifecycle.LifecycleOwner
    ) {
        Log.d("GamePlayViewModel", "📹 CameraX 초기화 시작")
        
        try {
            // ProcessCameraProvider 인스턴스 가져오기
            val cameraProvider = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(context).get()
            
            // 기존 카메라 바인딩 해제
            try {
                cameraProvider.unbindAll()
            } catch (e: Exception) {
                Log.w("GamePlayViewModel", "📹 기존 카메라 바인딩 해제 중 오류", e)
            }
            
            // 1) 숨겨진 Preview 생성 (카메라 활성화용)
            val preview = androidx.camera.core.Preview.Builder().build()
            
            // 2) VideoCapture 설정
            val recorder = androidx.camera.video.Recorder.Builder()
                .setQualitySelector(
                    androidx.camera.video.QualitySelector.from(
                        androidx.camera.video.Quality.SD,
                        androidx.camera.video.FallbackStrategy.lowerQualityOrHigherThan(androidx.camera.video.Quality.SD)
                    )
                )
                .build()
            videoCapture = androidx.camera.video.VideoCapture.withOutput(recorder)
            
            Log.d("GamePlayViewModel", "📹 VideoCapture 설정 완료")
            
            // 3) 카메라 선택
            val selector = androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA
            
            // 4) Preview + VideoCapture 함께 바인딩 (카메라 활성화 보장)
            try {
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    videoCapture
                )
                
                Log.d("GamePlayViewModel", "📹 CameraX 바인딩 완료")
                _cameraState.value = _cameraState.value.copy(isInitialized = true)
            } catch (e: Exception) {
                Log.e("GamePlayViewModel", "📹 CameraX 바인딩 실패", e)
                _cameraState.value = _cameraState.value.copy(error = "CameraX 바인딩 실패: ${e.message}")
            }
            
        } catch (e: Exception) {
            Log.e("GamePlayViewModel", "📹 CameraX 초기화 실패", e)
            _cameraState.value = _cameraState.value.copy(error = "CameraX 초기화 실패: ${e.message}")
        }
    }
    
    /**
     * CameraX 리소스 정리
     */
    fun clearCameraResources() {
        Log.d("GamePlayViewModel", "📹 CameraX 리소스 정리")
        try {
            stopCameraRecording()
        } catch (e: Exception) {
            Log.w("GamePlayViewModel", "📹 녹화 중지 중 오류 발생", e)
        }
        
        try {
            videoCapture = null
        } catch (e: Exception) {
            Log.w("GamePlayViewModel", "📹 VideoCapture 정리 중 오류 발생", e)
        }
        
        _cameraState.value = CameraState()
    }

    override fun onCleared() {
        super.onCleared()
        
        try {
            // CameraX 리소스 정리
            clearCameraResources()
        } catch (e: Exception) {
            Log.e("GamePlayViewModel", "📹 CameraX 리소스 정리 중 오류 발생", e)
        }
        
        try {
            when (gameMode) {
                GameMode.HARD -> {
                    // Hard 모드: 웹소켓 연결 해제
                    webSocketStreamer.stop()
                    rhythmCollector?.stopCollection()
                }
                GameMode.CHART_CREATION -> {
                    // 채보만들기 모드: CameraX 리소스 정리 (ViewModel에서 관리)
                    clearCameraResources()
                }
                else -> {
                    // Easy 모드: 특별한 정리 작업 없음
                }
            }
        } catch (e: Exception) {
            Log.e("GamePlayViewModel", "🎮 게임 모드별 리소스 정리 중 오류 발생", e)
        }
    }
    
}
