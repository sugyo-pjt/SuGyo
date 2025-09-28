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
import com.ssafy.a602.game.api.RhythmResultApi
import com.ssafy.a602.game.play.recorder.CoordinatesRecorder
import com.ssafy.a602.game.play.converter.MediaPipeToVec4Converter
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
    val isPaused: Boolean = false,
    // 실시간 판정 표시
    val currentGrade: String = "",
    val lastJudgment: String = "",
    val similarity: Float = 0f
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
    var avgSimilarity: Float = 0f,
    // 실시간 판정 표시용
    var lastGrade: String = "",
    var lastJudgment: String = "",
    var lastSimilarity: Float = 0f
)

@HiltViewModel
class GamePlayViewModel @Inject constructor(
    private val rhythmUploadService: RhythmUploadService,
    private val rhythmResultApi: RhythmResultApi,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) : ViewModel() {

    private lateinit var calc: GameScoreCalculator
    
    private val isUploading = java.util.concurrent.atomic.AtomicBoolean(false) // ✅ CAS 가드
    private var loopStarted = false // ✅ 루프 중복 방지
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
    
    // 🎯 좌표 수집기 (서버 전송용)
    private var coordinatesRecorder: CoordinatesRecorder? = null
    private var gameStartedAtMs: Long = 0L
    
    // 게임 통계
    private var gameStats = GameStats()
    
    // 현재 섹션 정보를 위한 변수들
    private var currentSections: List<com.ssafy.a602.game.data.SongSection> = emptyList()
    private var currentSectionIndex: Int = 0

    fun startGame(songId: String, totalWords: Int, mode: GameMode, playerPositionMs: () -> Long = { 0L }) {
        android.util.Log.d("GamePlayViewModel", "🎮 게임 시작: songId=$songId, mode=$mode, totalWords=$totalWords")
        this.songId = songId
        this.gameMode = mode
        this.currentMusicId = songId.toLongOrNull() ?: -1L
        this.playerPositionProvider = playerPositionMs
        
        // 게임 통계 초기화
        gameStats = GameStats()
        
        // 섹션 정보 로드
        loadSections()
        
        // Easy 모드일 때 하드모드와 동일한 실시간 판정 시스템 초기화
        if (mode == GameMode.EASY) {
            android.util.Log.d("GamePlayViewModel", "📊 Easy 모드: 하드모드와 동일한 실시간 판정 시스템 초기화")
            
            // 게임 시작 시간 기록
            gameStartedAtMs = System.currentTimeMillis()
            
            // PlayerClock 초기화
            playerClock = PlayerClock().apply {
                setPlayerPositionProvider(playerPositionMs)
                start()
            }
            
            // FeatureRingBuffer 초기화
            featureBuffer = FeatureRingBuffer()
            
            // LocalJudgeEngine 초기화
            localJudgeEngine = LocalJudgeEngine()
            
            // 🎯 좌표 수집기 초기화 (서버 전송용)
            coordinatesRecorder = CoordinatesRecorder(currentMusicId).apply {
                startSession(0L) // 음악 재생 시작을 0으로 설정
            }
            
            // AnswerTimeline 로드
            viewModelScope.launch {
                try {
                    answerTimeline = AnswerLoader.load(context, currentMusicId)
                    if (answerTimeline != null) {
                        android.util.Log.d("GamePlayViewModel", "✅ Easy 모드: 정답 타임라인 로드 성공: ${answerTimeline!!.frames.size}개 프레임")
                        startLoopIfNeeded() // ✅ 중복 방지
                    } else {
                        android.util.Log.e("GamePlayViewModel", "❌ Easy 모드: 정답 타임라인 로드 실패")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("GamePlayViewModel", "❌ Easy 모드: 정답 타임라인 로드 오류", e)
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
            
            android.util.Log.d("GamePlayViewModel", "📊 Easy 모드: 하드모드와 동일한 실시간 판정 시스템 초기화 완료")
        }
        
        // 🔥 하드 모드일 때 새로운 판정 시스템 초기화
        if (mode == GameMode.HARD) {
            android.util.Log.d("GamePlayViewModel", "🔥 Hard 모드: 새로운 판정 시스템 초기화 시작")
            
            // 게임 시작 시간 기록
            gameStartedAtMs = System.currentTimeMillis()
            
            // PlayerClock 초기화
            playerClock = PlayerClock().apply {
                setPlayerPositionProvider(playerPositionMs)
                start()
            }
            
            // FeatureRingBuffer 초기화
            featureBuffer = FeatureRingBuffer()
            
            // LocalJudgeEngine 초기화
            localJudgeEngine = LocalJudgeEngine()
            
            // 🎯 좌표 수집기 초기화 (서버 전송용)
            coordinatesRecorder = CoordinatesRecorder(currentMusicId).apply {
                startSession(0L) // 음악 재생 시작을 0으로 설정
            }
            
            // AnswerTimeline 로드
            viewModelScope.launch {
                try {
                    answerTimeline = AnswerLoader.load(context, currentMusicId)
                    if (answerTimeline != null) {
                        android.util.Log.d("GamePlayViewModel", "✅ 정답 타임라인 로드 성공: ${answerTimeline!!.frames.size}개 프레임")
                        startLoopIfNeeded() // ✅ 중복 방지
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

    // 🔥 Easy 모드: 프론트엔드에서 계산 (주석처리 - 하드모드와 동일한 로직 사용)
    fun onServerVerdict(isPerfect: Boolean, word: String) {
        // 이지모드 판정 로직 주석처리 - 하드모드와 동일한 실시간 판정 시스템 사용
        /*
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
        */
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
            
            // Easy 모드: 하드모드와 동일한 실시간 판정 시스템 사용
            val currentTime = playerClock?.nowMs() ?: 0L
            featureBuffer?.addFrame(pose, left, right, currentTime)
            
            android.util.Log.d("GamePlayViewModel", "📊 Easy 모드: 프레임 추가 완료 - currentTime=$currentTime, featureBuffer=${featureBuffer != null}")
            
            // 🎯 좌표 수집기에 데이터 추가 (서버 전송용)
            val musicTime = playerClock?.nowMs() ?: 0L // 음악 재생 시간 사용
            val (body, leftHand, rightHand) = MediaPipeToVec4Converter.convertForServer(
                bodyLm = pose,
                leftLm = left,
                rightLm = right,
                mirrorX = true,
                swapHands = false
            )
            coordinatesRecorder?.appendFrame(musicTime, body, leftHand, rightHand)
            
            // 기존 리듬 수집기도 유지 (호환성)
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
            
            android.util.Log.d("GamePlayViewModel", "🔥 Hard 모드: 프레임 추가 완료 - currentTime=$currentTime, featureBuffer=${featureBuffer != null}")
            
            // 🎯 좌표 수집기에 데이터 추가 (서버 전송용)
            val musicTime = playerClock?.nowMs() ?: 0L // 음악 재생 시간 사용
            val (body, leftHand, rightHand) = MediaPipeToVec4Converter.convertForServer(
                bodyLm = pose,
                leftLm = left,
                rightLm = right,
                mirrorX = true,
                swapHands = false
            )
            coordinatesRecorder?.appendFrame(musicTime, body, leftHand, rightHand)
            
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
    
    // 🔥 게임 완료 처리도 모드별로 다르게 (이지모드도 하드모드와 동일한 로직 사용)
    fun finishGame() {
        // 이지모드도 하드모드와 동일한 실시간 판정 시스템 사용
        // 서버에서 게임 완료 신호를 받으면 자동으로 처리됨
    }

    fun finishGameAndPost() {
        if (!isUploading.compareAndSet(false, true)) {
            android.util.Log.d("GamePlayViewModel", "이미 업로드 중 - 중복 호출 방지")
            return
        }

        _ui.value = _ui.value.copy(isPaused = true)

        viewModelScope.launch {
            try {
                _complete.value = _complete.value.copy(submitting = true, submitError = null)
                finishAndSendResult() // ✅ suspend 직접 호출
                _complete.value = _complete.value.copy(submitting = false, submitted = true)
            } catch (e: Exception) {
                _complete.value = _complete.value.copy(
                    submitting = false,
                    submitted = true,
                    submitError = e.message ?: "결과 전송 중 오류 발생"
                )
            } finally {
                isUploading.set(false)
            }
        }
    }
    
    // ✅ 루프 시작 중복 방지
    private fun startLoopIfNeeded() {
        if (loopStarted) return
        loopStarted = true
        startRealTimeLoop()
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
     * 섹션 정보 로드
     */
    private fun loadSections() {
        viewModelScope.launch {
            try {
                // GameDataManager를 통해 섹션 정보 가져오기
                val sections = com.ssafy.a602.game.data.GameDataManager.getSongSections(songId)
                currentSections = sections
                android.util.Log.d("GamePlayViewModel", "📝 섹션 정보 로드 완료: ${sections.size}개 섹션")
                
                // 섹션별 정보 로그
                sections.forEachIndexed { index, section ->
                    android.util.Log.d("GamePlayViewModel", "섹션[$index]: '${section.text}' (${section.startTime}s~${section.endTime}s)")
                }
            } catch (e: Exception) {
                android.util.Log.e("GamePlayViewModel", "❌ 섹션 정보 로드 실패", e)
                currentSections = emptyList()
            }
        }
    }
    
    /**
     * 현재 시간에 해당하는 섹션 인덱스 찾기
     */
    private fun findCurrentSectionIndex(currentTimeMs: Long): Int {
        val currentTimeSeconds = currentTimeMs / 1000f
        
        if (currentSections.isEmpty()) {
            return 0
        }
        
        // 현재 시간이 첫 번째 섹션 시작 시간보다 작으면 0 반환
        if (currentTimeSeconds < currentSections[0].startTime) {
            return 0
        }
        
        // 현재 시간이 마지막 섹션의 종료 시간보다 크면 마지막 인덱스 반환
        val lastSection = currentSections.last()
        if (currentTimeSeconds >= lastSection.endTime) {
            return currentSections.size - 1
        }
        
        // 현재 시간이 포함되는 섹션 찾기
        for (i in currentSections.indices) {
            val section = currentSections[i]
            if (currentTimeSeconds >= section.startTime && currentTimeSeconds < section.endTime) {
                return i
            }
        }
        
        // 위 조건에 맞지 않으면 가장 가까운 다음 섹션의 이전 인덱스 반환
        for (i in currentSections.indices) {
            if (currentTimeSeconds < currentSections[i].startTime) {
                return maxOf(0, i - 1)
            }
        }
        
        return currentSections.size - 1
    }
    
    /**
     * 현재 섹션이 가사가 있는 섹션인지 확인 (하드 모드용)
     */
    private fun isCurrentSectionWithLyrics(currentTimeMs: Long): Boolean {
        val currentTimeSeconds = currentTimeMs / 1000f
        val sectionIndex = findCurrentSectionIndex(currentTimeMs)
        
        if (sectionIndex >= currentSections.size) {
            return false
        }
        
        val currentSection = currentSections[sectionIndex]
        
        // 가사가 비어있지 않고, 현재 시간이 섹션 범위 내에 있는지 확인
        val hasLyrics = currentSection.text.isNotBlank() && currentSection.text.isNotEmpty()
        val isInTimeRange = currentTimeSeconds >= currentSection.startTime && currentTimeSeconds < currentSection.endTime
        
        android.util.Log.d("GamePlayViewModel", "🎵 가사 확인: section='${currentSection.text}', hasLyrics=$hasLyrics, isInTimeRange=$isInTimeRange, time=${currentTimeSeconds}s (${currentSection.startTime}s~${currentSection.endTime}s)")
        
        return hasLyrics && isInTimeRange
    }
    
    /**
     * 현재 시간이 정답 단어 타이밍에 해당하는지 확인 (이지 모드용)
     */
    private fun isCurrentTimeInAnswerWordTiming(currentTimeMs: Long): Boolean {
        val currentTimeSeconds = currentTimeMs / 1000f
        val sectionIndex = findCurrentSectionIndex(currentTimeMs)
        
        if (sectionIndex >= currentSections.size) {
            return false
        }
        
        val currentSection = currentSections[sectionIndex]
        
        // 정답 정보가 있는지 확인
        if (currentSection.correctInfo.isEmpty()) {
            android.util.Log.d("GamePlayViewModel", "🎯 정답 정보 없음: section='${currentSection.text}'")
            return false
        }
        
        // 현재 시간이 정답 단어 타이밍에 해당하는지 확인
        val isInAnswerTiming = currentSection.correctInfo.any { correct ->
            val actionStartTime = parseTimeToSeconds(correct.actionStartedAt)
            val actionEndTime = parseTimeToSeconds(correct.actionEndedAt)
            
            val isInRange = currentTimeSeconds >= actionStartTime && currentTimeSeconds <= actionEndTime
            
            android.util.Log.d("GamePlayViewModel", "🎯 정답 타이밍 확인: section='${currentSection.text}', correct=${correct.correctStartedIndex}~${correct.correctEndedIndex}, actionTime=${actionStartTime}s~${actionEndTime}s, currentTime=${currentTimeSeconds}s, isInRange=$isInRange")
            
            isInRange
        }
        
        android.util.Log.d("GamePlayViewModel", "🎯 정답 단어 타이밍 확인: section='${currentSection.text}', isInAnswerTiming=$isInAnswerTiming, time=${currentTimeSeconds}s")
        
        return isInAnswerTiming
    }
    
    /**
     * 시간 문자열을 초 단위로 변환
     */
    private fun parseTimeToSeconds(timeString: String): Float {
        return try {
            timeString.toFloat()
        } catch (e: Exception) {
            android.util.Log.w("GamePlayViewModel", "시간 파싱 실패: $timeString", e)
            0f
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
        
        android.util.Log.d("GamePlayViewModel", "🎯 tick 실행: currentTime=$currentTime, answerFrame=$answerFrame, userFrame=$userFrame, gameMode=$gameMode")
        
        // 게임 모드에 따라 다른 타이밍 로직 적용
        val shouldPerformComparison = when (gameMode) {
            com.ssafy.a602.game.data.GameMode.EASY -> {
                // 이지 모드: 정답 단어 타이밍에서만 비교
                val isInAnswerTiming = isCurrentTimeInAnswerWordTiming(currentTime)
                android.util.Log.d("GamePlayViewModel", "📊 이지 모드: 정답 단어 타이밍 확인=$isInAnswerTiming")
                isInAnswerTiming
            }
            com.ssafy.a602.game.data.GameMode.HARD -> {
                // 하드 모드: 가사가 있는 섹션에서만 비교
                val isInLyricsSection = isCurrentSectionWithLyrics(currentTime)
                android.util.Log.d("GamePlayViewModel", "🔥 하드 모드: 가사 섹션 확인=$isInLyricsSection")
                isInLyricsSection
            }
            else -> {
                android.util.Log.w("GamePlayViewModel", "⚠️ 알 수 없는 게임 모드: $gameMode")
                false
            }
        }
        
        if (!shouldPerformComparison) {
            android.util.Log.d("GamePlayViewModel", "⏭️ 유사도 비교 건너뛰기: gameMode=$gameMode")
            return
        }
        
        android.util.Log.d("GamePlayViewModel", "✅ 유사도 비교 수행: gameMode=$gameMode")
        
        // 새로운 판정 시스템 사용
        val userFrames = convertToMotionFrames(userFrame)
        val answerFrames = convertAnswerFrameToMotionFrames(answerFrame)
        
        // JsonSimilarityComparator를 사용한 유사도 계산
        val similarity = JsonSimilarityComparator.calculateMotionSimilarity(userFrames, answerFrames, 640, 480)
        
        // LocalJudgeEngine을 사용한 판정
        val judgment = judge.judgeByRatio(similarity)
        val grade = judgment.name
        
        android.util.Log.d("GamePlayViewModel", "🎯 판정 결과: similarity=$similarity, grade=$grade")
        
        // 통계 업데이트
        updateGameStats(grade, similarity)
        
        
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
        
        // 실시간 판정 저장
        gameStats.lastGrade = grade
        gameStats.lastJudgment = grade
        gameStats.lastSimilarity = similarity
        
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
        
        android.util.Log.d("GamePlayViewModel", "🎯 UI 업데이트: grade=$grade, similarity=$similarity, combo=${gameStats.currentCombo}")
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
            missCount = gameStats.missCount,
            // 실시간 판정 표시
            currentGrade = gameStats.lastGrade,
            lastJudgment = gameStats.lastJudgment,
            similarity = gameStats.lastSimilarity
        )
    }
    
    /**
     * HARD 모드 결과 생성
     */
    private fun createHardModeResult() {
        try {
            // 기존 SimilarityJudge 로직을 사용한 등급 계산
            val accuracy = ((gameStats.perfectCount + gameStats.goodCount) * 100 / gameStats.totalCount.coerceAtLeast(1))
            val grade = calculateGradeFromAccuracy(accuracy)
            
            val result = com.ssafy.a602.game.result.GameResultUi(
                songTitle = "노래 제목", // TODO: 실제 노래 제목으로 교체
                score = gameStats.totalScore,
                accuracyPercent = accuracy,
                grade = grade,
                maxCombo = gameStats.maxCombo,
                correctCount = gameStats.perfectCount + gameStats.goodCount,
                missCount = gameStats.missCount,
                comboMultiplier = 1.0,
                isNewRecord = false,
                missWords = emptyList(), // HARD 모드에서는 단어 목록 없음
                accepted = true,
                isPersonalBest = false,
                rankUpdated = false,
                serverScoreEcho = 0, // TODO: 서버 응답에서 받아오도록 수정
                // HARD 모드용 추가 필드들
                perfectCount = gameStats.perfectCount,
                goodCount = gameStats.goodCount,
                totalCount = gameStats.totalCount,
                avgSimilarity = gameStats.avgSimilarity,
                gameMode = "HARD"
            )
            
            android.util.Log.d("GamePlayViewModel", "🎯 HARD 모드 결과 생성 완료: score=${result.score}, perfect=${result.perfectCount}, good=${result.goodCount}, miss=${result.missCount}")
            
            // TODO: 결과 화면으로 네비게이션
            // navController.navigate("game_result/${result}")
            
        } catch (e: Exception) {
            android.util.Log.e("GamePlayViewModel", "🎯 HARD 모드 결과 생성 실패", e)
        }
    }
    
    /**
     * 게임 종료 시 서버로 결과 전송
     */
    private suspend fun finishAndSendResult() {
        if (coordinatesRecorder == null) {
            android.util.Log.w("GamePlayViewModel", "좌표 수집기가 없음")
            return
        }
        try {
            val coordinatesDto = coordinatesRecorder!!.buildDto()
            val request = RhythmResultRequest(
                clientCoordinates = listOf(coordinatesDto),
                clientCalculateScore = gameStats.totalScore
            )
            val response = rhythmResultApi.postResult("", request)
            if (response.isSuccessful) {
                _ui.value = _ui.value.copy(submitted = true)
            } else {
                _ui.value = _ui.value.copy(error = "서버 전송 실패: ${response.code()}")
            }
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(error = "서버 전송 오류: ${e.message}")
        }
    }
    
    /**
     * 정확도 기반 등급 계산 (SimilarityJudge 로직 참고)
     */
    private fun calculateGradeFromAccuracy(accuracy: Int): String {
        return when {
            accuracy >= 95 -> "S"  // 95% 이상: S등급
            accuracy >= 90 -> "A"  // 90% 이상: A등급
            accuracy >= 80 -> "B"  // 80% 이상: B등급
            accuracy >= 70 -> "C"  // 70% 이상: C등급
            else -> "F"            // 그 외: F등급
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        
        // 모든 모드에서 리듬 수집기 정리
        rhythmCollector?.stopCollection()
        
        // 이지모드와 하드모드 모두 동일한 정리 로직 적용
        playerClock?.stop()
        featureBuffer?.clear()
        coordinatesRecorder?.reset()
    }
    
}
