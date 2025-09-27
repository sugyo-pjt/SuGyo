package com.ssafy.a602.game.play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a602.game.data.GameDataManager
import com.ssafy.a602.game.data.GameMode
import com.ssafy.a602.game.play.input.LM
// 웹소켓 관련 import 제거됨
import com.ssafy.a602.game.score.GameScoreCalculator
import com.ssafy.a602.game.score.JudgmentType as ScoreJudgmentType
import com.ssafy.a602.game.play.collector.RhythmCollector
import com.ssafy.a602.game.play.collector.MediaPipeToRhythmConverter
import com.ssafy.a602.game.play.dto.*
import com.ssafy.a602.game.play.service.RhythmUploadService
import com.ssafy.a602.game.play.answer.AnswerLoader
import com.ssafy.a602.game.play.answer.AnswerTimeline
import com.ssafy.a602.game.play.clock.PlayerClock
import com.ssafy.a602.game.play.judge.FeatureRingBuffer
import com.ssafy.a602.game.play.judge.LocalJudgeEngine
import com.ssafy.a602.game.play.judge.FrameFeature
import com.ssafy.a602.game.play.judge.JsonSimilarityComparator
import com.ssafy.a602.game.play.judge.MotionFrame
import com.ssafy.a602.game.play.judge.Pose
import com.ssafy.a602.game.play.judge.Coordinate
import com.ssafy.a602.game.play.judge.BodyPart
import com.ssafy.a602.game.api.RhythmVerifyApi
import android.content.Context
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

data class GameStats(
    var perfectCount: Int = 0,
    var goodCount: Int = 0,
    var missCount: Int = 0,
    var totalCount: Int = 0,
    var maxCombo: Int = 0,
    var currentCombo: Int = 0,
    var totalScore: Int = 0,
    var avgSimilarity: Float = 0f
)

@HiltViewModel
class GamePlayViewModel @Inject constructor(
    private val rhythmUploadService: RhythmUploadService,
    private val rhythmVerifyApi: RhythmVerifyApi,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
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
    
    // 새로운 판정 시스템 컴포넌트
    private var answerTimeline: AnswerTimeline? = null
    private var playerClock: PlayerClock? = null
    private var featureBuffer: FeatureRingBuffer? = null
    private var localJudgeEngine: LocalJudgeEngine? = null
    
    // 게임 통계
    private var gameStats = GameStats()
    private val sampleData = mutableListOf<SampleData>()

    fun startGame(songId: String, totalWords: Int, mode: GameMode, playerPositionMs: () -> Long = { 0L }) {
        android.util.Log.d("GamePlayViewModel", "🎮 게임 시작: songId=$songId, mode=$mode, totalWords=$totalWords")
        this.songId = songId
        this.gameMode = mode
        this.currentMusicId = songId.toLongOrNull() ?: -1L
        this.playerPositionProvider = playerPositionMs
        
        // 게임 통계 초기화
        gameStats = GameStats()
        sampleData.clear()
        
        // Easy 모드일 때 프론트엔드 계산기와 리듬 수집기 초기화
        if (mode == GameMode.EASY) {
            android.util.Log.d("GamePlayViewModel", "📊 Easy 모드: 프론트엔드 계산기 초기화")
            calc = GameScoreCalculator(songId = songId, totalWords = totalWords, baseScore = 100)
            
            // Easy 모드에서도 리듬 수집기 초기화 (서버 전송용)
            android.util.Log.d("GamePlayViewModel", "📊 Easy 모드: 리듬 수집기 초기화")
            rhythmCollector = RhythmCollector(
                musicId = currentMusicId.toInt(),
                coroutineScope = viewModelScope
            )
            rhythmCollector?.startCollection()
            
            viewModelScope.launch {
                rhythmCollector?.onTypeChanged(SegmentType.PLAY, 0L)
                android.util.Log.d("GamePlayViewModel", "📊 Easy 모드: PLAY 세그먼트 시작")
            }
        }
        
        // 🔥 하드 모드일 때 새로운 판정 시스템 초기화
        if (mode == GameMode.HARD) {
            android.util.Log.d("GamePlayViewModel", "🔥 Hard 모드: 새로운 판정 시스템 초기화 시작")
            
            // PlayerClock 초기화
            playerClock = PlayerClock().apply {
                setPlayerPositionProvider(playerPositionMs)
                start()
            }
            
            // FeatureRingBuffer 초기화
            featureBuffer = FeatureRingBuffer()
            
            // LocalJudgeEngine 초기화
            localJudgeEngine = LocalJudgeEngine()
            
            // AnswerTimeline 로드
            viewModelScope.launch {
                try {
                    answerTimeline = AnswerLoader.load(context, currentMusicId)
                    if (answerTimeline != null) {
                        android.util.Log.d("GamePlayViewModel", "✅ 정답 타임라인 로드 성공: ${answerTimeline!!.frames.size}개 프레임")
                        startRealTimeLoop()
                    } else {
                        android.util.Log.e("GamePlayViewModel", "❌ 정답 타임라인 로드 실패")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("GamePlayViewModel", "❌ 정답 타임라인 로드 오류", e)
                }
            }
            
            // 기존 리듬 수집기도 유지 (호환성)
            rhythmCollector = RhythmCollector(
                musicId = currentMusicId.toInt(),
                coroutineScope = viewModelScope
            )
            rhythmCollector?.startCollection()
            
            viewModelScope.launch {
                rhythmCollector?.onTypeChanged(SegmentType.PLAY, 0L)
            }
            
            android.util.Log.d("GamePlayViewModel", "🔥 Hard 모드: 새로운 판정 시스템 초기화 완료")
        }
        
        _ui.value = GameUiState()
        _complete.value = CompleteUiState()
    }
    
    // 웹소켓 관련 메서드들 제거됨 - 로컬 판정으로 대체

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
    
    // MediaPipe 결과를 모드별로 처리
    fun onLandmarks(pose: List<LM?>, left: List<LM?>, right: List<LM?>) {
        android.util.Log.d("GamePlayViewModel", "🎯 onLandmarks 호출: gameMode=$gameMode, pose=${pose.size}, left=${left.size}, right=${right.size}")
        
        // 데이터 유효성 검사
        if (pose.isEmpty() && left.isEmpty() && right.isEmpty()) {
            android.util.Log.w("GamePlayViewModel", "⚠️ 모든 MediaPipe 데이터가 비어있음!")
            return
        }
        
        if (gameMode == GameMode.EASY) {
            android.util.Log.d("GamePlayViewModel", "📊 Easy 모드: MediaPipe 데이터 수신 - pose=${pose.size}, left=${left.size}, right=${right.size}")
            
            // Easy 모드: 리듬 수집기에 데이터 전달 (서버 전송용)
            val poses = MediaPipeToRhythmConverter.convertToPoses(pose, left, right)
            viewModelScope.launch {
                val positionMs = withContext(Dispatchers.Main) {
                    playerPositionProvider?.invoke() ?: 0L
                }
                android.util.Log.v("GamePlayViewModel", "📊 Easy 모드: 리듬 수집기에 프레임 전달 - positionMs=$positionMs, poses=${poses.size}")
                rhythmCollector?.addFrameToBuffer(poses, positionMs)
            }
        } else if (gameMode == GameMode.HARD) {
            android.util.Log.d("GamePlayViewModel", "🔥 Hard 모드: MediaPipe 데이터 수신 - pose=${pose.size}, left=${left.size}, right=${right.size}")
            
            // Hard 모드: 새로운 판정 시스템에 프레임 추가
            val currentTime = playerClock?.nowMs() ?: 0L
            featureBuffer?.addFrame(pose, left, right, currentTime)
            
            // 기존 리듬 수집기도 유지 (호환성)
            val poses = MediaPipeToRhythmConverter.convertToPoses(pose, left, right)
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
        
        // 하드 모드일 때 로컬 일시정지 처리
        if (gameMode == GameMode.HARD) {
            // TODO: 로컬 일시정지 처리 로직
            
            // 🔥 리듬 수집기에 세그먼트 타입 변경 알림
            val positionMs = playerPositionProvider?.invoke() ?: 0L
            val segmentType = if (currentPaused) SegmentType.PAUSE else SegmentType.RESUME
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
        if (_complete.value.submitting) return // 더블탭 방지
        
        // 중복 호출 방지
        if (isUploading) {
            android.util.Log.d("GamePlayViewModel", "🎯 이미 업로드 중 - 중복 호출 방지")
            return
        }
        
        // 게임 완료 상태로 변경하여 ExoPlayer 정지 신호
        _ui.value = _ui.value.copy(isPaused = true)
        
        if (gameMode == GameMode.EASY) {
            android.util.Log.d("GamePlayViewModel", "📊 Easy 모드: 리듬 데이터 수집 및 결과 전송")
            // Easy 모드: 리듬 데이터 수집 후 결과 전송
            isUploading = true
            viewModelScope.launch {
                _complete.value = _complete.value.copy(submitting = true, submitError = null)
                
                try {
                    // 리듬 데이터 수집 완료
                    android.util.Log.d("GamePlayViewModel", "📊 Easy 모드: 리듬 데이터 수집 완료 요청")
                    val rhythmData = rhythmCollector?.onSongEnd()
                    android.util.Log.d("GamePlayViewModel", "📊 Easy 모드: 리듬 데이터 수집 결과 - ${if (rhythmData != null) "성공" else "실패"}")
                    
                    if (rhythmData != null) {
                        android.util.Log.d("GamePlayViewModel", "📊 Easy 모드: 리듬 데이터 수집 완료 - musicId=${rhythmData.musicId}, segments=${rhythmData.allFrames.size}")
                        
                        // 리듬 데이터 업로드 API 호출
                        val uploadResult = rhythmUploadService.uploadRhythmDataWithRetry(
                            request = rhythmData
                        )
                        
                        android.util.Log.d("GamePlayViewModel", "📊 Easy 모드: 리듬 데이터 업로드 결과 - ${if (uploadResult.isSuccess) "성공" else "실패"}")
                        if (uploadResult.isSuccess) {
                            _complete.value = _complete.value.copy(
                                submitting = false,
                                submitted = true,
                                isBestRecord = false // TODO: 서버 응답에서 확인
                            )
                        } else {
                            _complete.value = _complete.value.copy(
                                submitting = false,
                                submitted = true,
                                submitError = uploadResult.exceptionOrNull()?.message ?: "리듬 데이터 업로드 실패"
                            )
                        }
                    } else {
                        android.util.Log.e("GamePlayViewModel", "📊 Easy 모드: 리듬 데이터 수집 실패")
                        _complete.value = _complete.value.copy(
                            submitting = false,
                            submitted = true,
                            submitError = "리듬 데이터 수집 실패"
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("GamePlayViewModel", "📊 Easy 모드: 예외 발생", e)
                    _complete.value = _complete.value.copy(
                        submitting = false,
                        submitted = true,
                        submitError = e.message ?: "리듬 데이터 업로드 실패"
                    )
                } finally {
                    isUploading = false
                }
            }
        } else {
            android.util.Log.d("GamePlayViewModel", "🔥 Hard 모드: 새로운 검증 시스템으로 결과 전송")
            // 🔥 Hard 모드: 새로운 검증 시스템 사용
            isUploading = true
            viewModelScope.launch {
                _complete.value = _complete.value.copy(submitting = true, submitError = null)
                
                try {
                    // 검증 요청 생성
                    val verifyRequest = createVerifyRequest()
                    android.util.Log.d("GamePlayViewModel", "🔥 Hard 모드: 검증 요청 생성 완료 - musicId=${verifyRequest.musicId}, samples=${verifyRequest.samples.size}")
                    
                    // 검증 API 호출
                    val response = rhythmVerifyApi.verifyRhythm(verifyRequest)
                    
                    if (response.isSuccessful) {
                        val verifyResponse = response.body()
                        android.util.Log.d("GamePlayViewModel", "🔥 Hard 모드: 검증 성공 - serverScore=${verifyResponse?.serverScore}, accuracy=${verifyResponse?.accuracy}")
                        
                        // 서버 점수로 UI 업데이트
                        if (verifyResponse != null) {
                            _ui.value = _ui.value.copy(
                                score = verifyResponse.serverScore,
                                grade = verifyResponse.rank
                            )
                        }
                        
                        _complete.value = _complete.value.copy(
                            submitting = false,
                            submitted = true,
                            isBestRecord = false // TODO: 서버 응답에서 확인
                        )
                    } else {
                        android.util.Log.e("GamePlayViewModel", "🔥 Hard 모드: 검증 실패 - ${response.code()}")
                        _complete.value = _complete.value.copy(
                            submitting = false,
                            submitted = true,
                            submitError = "검증 실패: ${response.code()}"
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("GamePlayViewModel", "🔥 Hard 모드: 검증 오류", e)
                    _complete.value = _complete.value.copy(
                        submitting = false,
                        submitted = true,
                        submitError = e.message ?: "검증 요청 실패"
                    )
                } finally {
                    isUploading = false
                }
            }
        }
    }
    
    /**
     * 실시간 판정 루프 시작 (16ms 간격)
     */
    private fun startRealTimeLoop() {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                while (true) {
                    tick()
                    kotlinx.coroutines.delay(16) // 60Hz
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                android.util.Log.d("GamePlayViewModel", "실시간 루프 취소됨")
                throw e
            } catch (e: Exception) {
                android.util.Log.e("GamePlayViewModel", "실시간 루프 오류", e)
            }
        }
    }
    
    /**
     * 실시간 판정 틱
     */
    private fun tick() {
        val currentTime = playerClock?.nowMs() ?: return
        val answerFrame = answerTimeline?.frameAt(currentTime) ?: return
        val userFrame = featureBuffer?.getLatestOrNearest(currentTime) ?: return
        val judge = localJudgeEngine ?: return
        
        // 새로운 판정 시스템 사용
        val userFrames = convertToMotionFrames(userFrame)
        val answerFrames = convertAnswerFrameToMotionFrames(answerFrame)
        
        // JsonSimilarityComparator를 사용한 유사도 계산
        val similarity = JsonSimilarityComparator.calculateMotionSimilarity(userFrames, answerFrames, 640, 480)
        
        // LocalJudgeEngine을 사용한 판정
        val judgment = judge.judgeByRatio(similarity)
        val grade = judgment.name
        
        // 통계 업데이트
        updateGameStats(grade, similarity)
        
        // 샘플 데이터 수집 (300ms 간격)
        if (shouldCollectSample()) {
            sampleData.add(SampleData(currentTime, similarity))
        }
        
        // UI 업데이트
        updateUI()
    }
    
    /**
     * FrameFeature를 MotionFrame으로 변환
     */
    private fun convertToMotionFrames(userFrame: FrameFeature): List<MotionFrame> {
        val poses = mutableListOf<Pose>()
        
        // BODY 포즈 추가
        val bodyCoordinates = convertFloatArrayToCoordinates(userFrame.pose)
        poses.add(Pose(BodyPart.BODY, bodyCoordinates))
        
        // LEFT_HAND 포즈 추가
        val leftCoordinates = convertFloatArrayToCoordinates(userFrame.left)
        poses.add(Pose(BodyPart.LEFT_HAND, leftCoordinates))
        
        // RIGHT_HAND 포즈 추가
        val rightCoordinates = convertFloatArrayToCoordinates(userFrame.right)
        poses.add(Pose(BodyPart.RIGHT_HAND, rightCoordinates))
        
        return listOf(MotionFrame(0, poses))
    }
    
    /**
     * AnswerFrame을 MotionFrame으로 변환
     */
    private fun convertAnswerFrameToMotionFrames(answerFrame: com.ssafy.a602.game.play.answer.AnswerFrame): List<MotionFrame> {
        val poses = mutableListOf<Pose>()
        
        // BODY 포즈 추가
        val bodyCoordinates = convertFloatArrayToCoordinates(answerFrame.pose)
        poses.add(Pose(BodyPart.BODY, bodyCoordinates))
        
        // LEFT_HAND 포즈 추가
        val leftCoordinates = convertFloatArrayToCoordinates(answerFrame.left)
        poses.add(Pose(BodyPart.LEFT_HAND, leftCoordinates))
        
        // RIGHT_HAND 포즈 추가
        val rightCoordinates = convertFloatArrayToCoordinates(answerFrame.right)
        poses.add(Pose(BodyPart.RIGHT_HAND, rightCoordinates))
        
        return listOf(MotionFrame(0, poses))
    }
    
    /**
     * FloatArray를 Coordinate 리스트로 변환
     */
    private fun convertFloatArrayToCoordinates(floatArray: FloatArray): List<Coordinate> {
        val coordinates = mutableListOf<Coordinate>()
        
        // FloatArray를 4개씩 묶어서 Coordinate로 변환 (x, y, z, w)
        for (i in floatArray.indices step 4) {
            val x = if (i < floatArray.size) floatArray[i].toDouble() else 0.0
            val y = if (i + 1 < floatArray.size) floatArray[i + 1].toDouble() else 0.0
            val z = if (i + 2 < floatArray.size) floatArray[i + 2].toDouble() else 0.0
            val w = if (i + 3 < floatArray.size) floatArray[i + 3].toDouble() else 1.0
            
            coordinates.add(Coordinate(x, y, z, w))
        }
        
        return coordinates
    }
    
    /**
     * GameSessionContext 생성 (점수 계산용)
     */
    private fun createGameSessionContext(): com.ssafy.a602.game.play.judge.GameSessionContext {
        return com.ssafy.a602.game.play.judge.GameSessionContext(
            userId = "current_user",
            musicId = currentMusicId,
            webSocketSessionId = "local_session",
            lastNoteTimestamp = 0f
        ).apply {
            // 현재 콤보 설정
            combo.set(gameStats.currentCombo)
        }
    }
    
    /**
     * 게임 통계 업데이트
     */
    private fun updateGameStats(grade: String, similarity: Float) {
        gameStats.totalCount++
        gameStats.avgSimilarity = (gameStats.avgSimilarity * (gameStats.totalCount - 1) + similarity) / gameStats.totalCount
        
        when (grade) {
            "PERFECT" -> {
                gameStats.perfectCount++
                gameStats.currentCombo++
                // LocalJudgeEngine을 사용한 점수 계산
                val judgment = com.ssafy.a602.game.play.judge.Judgment.PERFECT
                val points = localJudgeEngine?.calculatePoints(judgment, createGameSessionContext()) ?: 100
                gameStats.totalScore += points
            }
            "GOOD" -> {
                gameStats.goodCount++
                gameStats.currentCombo++
                // LocalJudgeEngine을 사용한 점수 계산
                val judgment = com.ssafy.a602.game.play.judge.Judgment.GOOD
                val points = localJudgeEngine?.calculatePoints(judgment, createGameSessionContext()) ?: 70
                gameStats.totalScore += points
            }
            "MISS" -> {
                gameStats.missCount++
                gameStats.currentCombo = 0
            }
        }
        
        gameStats.maxCombo = maxOf(gameStats.maxCombo, gameStats.currentCombo)
    }
    
    /**
     * 샘플 데이터 수집 여부 확인 (300ms 간격)
     */
    private fun shouldCollectSample(): Boolean {
        val currentTime = playerClock?.nowMs() ?: 0L
        val lastSampleTime = sampleData.lastOrNull()?.timestamp ?: 0L
        return currentTime - lastSampleTime >= 300
    }
    
    /**
     * UI 상태 업데이트
     */
    private fun updateUI() {
        val currentState = _ui.value
        _ui.value = currentState.copy(
            score = gameStats.totalScore,
            combo = gameStats.currentCombo,
            maxCombo = gameStats.maxCombo,
            correctCount = gameStats.perfectCount + gameStats.goodCount,
            missCount = gameStats.missCount
        )
    }
    
    /**
     * 검증 요청 생성
     */
    private fun createVerifyRequest(): RhythmVerifyRequest {
        val currentTime = playerClock?.nowMs() ?: 0L
        val startTime = answerTimeline?.startMs ?: 0L
        
        return RhythmVerifyRequest(
            musicId = currentMusicId,
            answerHash = answerTimeline?.answerHash,
            judgeVersion = answerTimeline?.judgeVersion,
            startedAt = startTime,
            endedAt = currentTime,
            summary = GameSummary(
                avgSim = gameStats.avgSimilarity,
                perfectRate = if (gameStats.totalCount > 0) gameStats.perfectCount.toFloat() / gameStats.totalCount else 0f,
                goodRate = if (gameStats.totalCount > 0) gameStats.goodCount.toFloat() / gameStats.totalCount else 0f,
                missRate = if (gameStats.totalCount > 0) gameStats.missCount.toFloat() / gameStats.totalCount else 0f,
                maxCombo = gameStats.maxCombo,
                clientScore = gameStats.totalScore
            ),
            samples = sampleData.toList(),
            featureSpecVersion = "1.0",
            device = DeviceInfo(
                model = android.os.Build.MODEL,
                os = "Android ${android.os.Build.VERSION.RELEASE}"
            )
        )
    }
    
    override fun onCleared() {
        super.onCleared()
        
        // 모든 모드에서 리듬 수집기 정리
        rhythmCollector?.stopCollection()
        
        if (gameMode == GameMode.HARD) {
            // 새로운 판정 시스템 정리
            playerClock?.stop()
            featureBuffer?.clear()
        }
    }
    
}
