package com.ssafy.a602.game.play

import android.os.SystemClock
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ExperimentalMirrorMode
import androidx.camera.core.ImageProxy
import androidx.media3.common.C
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.ssafy.a602.game.GameTheme
import com.ssafy.a602.game.CameraPreview
import com.ssafy.a602.game.data.GameDataManager
import com.ssafy.a602.game.data.GameMode
import com.ssafy.a602.game.data.SongProgress
import com.ssafy.a602.game.utils.TimeParsing
import com.ssafy.a602.game.play.input.DynamicLandmarkBuffer
import com.ssafy.a602.game.play.input.LandmarkResultHandler
import com.ssafy.a602.game.play.input.WordWindowUploader
import com.ssafy.a602.game.result.GameResultUi
import com.ssafy.a602.game.time.TimelineTick
import com.ssafy.a602.game.time.TimelineViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import com.ssafy.a602.game.ui.modern.*
import com.ssafy.a602.game.data.SongSection
import com.ssafy.a602.game.api.dto.CorrectDto

/* ========== Utility Functions ========== */

/**
 * 현재 시간에 해당하는 수어 하이라이팅 정보를 반환
 * 하드 모드에서는 하이라이팅을 비활성화
 */
private fun getCurrentSignHighlight(
    currentSection: SongSection?,
    currentTime: Float,
    currentSectionIndex: Int,
    gameMode: GameMode
): List<Pair<Int, Int>> {
    if (currentSection == null) {
        Log.d("GamePlayScreen", "getCurrentSignHighlight: currentSection is null")
        return emptyList()
    }
    
    // 하드 모드에서는 하이라이팅 비활성화
    if (gameMode == GameMode.HARD) {
        Log.d("GamePlayScreen", "Hard mode: 하이라이팅 비활성화")
        return emptyList()
    }
    
    val sectionStartTime = currentSection.startTime
    val isFirstSection = currentSectionIndex == 0 // 섹션 인덱스 기반으로 첫 소절 판단
    
    Log.d("GamePlayScreen", "getCurrentSignHighlight: sectionStartTime=$sectionStartTime, currentSectionIndex=$currentSectionIndex, isFirstSection=$isFirstSection, currentTime=$currentTime, correctInfoCount=${currentSection.correctInfo.size}")
    
    return currentSection.correctInfo.mapNotNull { correct ->
        val actionStartTime = parseTimeToSeconds(correct.actionStartedAt)
        val actionEndTime = parseTimeToSeconds(correct.actionEndedAt)
        
        // 첫 소절의 경우 게임 시작과 동시에 빨간색 표시 (전주부터)
        // 다른 소절의 경우 현재 시간이 수어 액션 시작 시간 이후인지 확인 (끝나도 계속 빨간색 유지)
        
        val shouldHighlight = if (isFirstSection) {
            // 첫 소절의 경우: 게임 시작(0초)부터 빨간색 표시
            true
        } else {
            // 다른 소절의 경우: 수어 액션 시작 시간 이후부터 빨간색 표시
            currentTime >= actionStartTime
        }
        
        if (shouldHighlight) {
            Log.d("GamePlayScreen", "Highlighting: correctStartedIndex=${correct.correctStartedIndex}, correctEndedIndex=${correct.correctEndedIndex}")
            Pair(correct.correctStartedIndex, correct.correctEndedIndex)
        } else {
            Log.d("GamePlayScreen", "Not highlighting: currentTime=$currentTime < actionStartTime=$actionStartTime")
            null
        }
    }
}

/**
 * 가사 텍스트에 하이라이팅을 적용한 AnnotatedString 생성
 * 기본적으로 흰색으로 표시하고, 수어 타이밍에 해당하는 부분만 빨간색으로 표시
 */
private fun createHighlightedLyrics(
    text: String,
    highlights: List<Pair<Int, Int>>
): AnnotatedString {
    Log.d("GamePlayScreen", "createHighlightedLyrics: text='$text', highlights=$highlights")
    
    return buildAnnotatedString {
        var lastIndex = 0
        
        highlights.sortedBy { it.first }.forEach { (start, end) ->
            Log.d("GamePlayScreen", "Processing highlight: start=$start, end=$end, text.length=${text.length}")
            
            // 하이라이트 이전 텍스트 추가 (흰색)
            if (start > lastIndex) {
                withStyle(style = SpanStyle(color = Color.White)) {
                    append(text.substring(lastIndex, start))
                }
            }
            
            // 하이라이트된 텍스트 추가 (빨간색) - 범위를 한 글자 더 확장
            val extendedEnd = (end + 1).coerceAtMost(text.length)
            withStyle(style = SpanStyle(color = Color(0xFFFF4444))) {
                append(text.substring(start, extendedEnd))
            }
            
            lastIndex = extendedEnd
        }
        
        // 마지막 하이라이트 이후 텍스트 추가 (흰색)
        if (lastIndex < text.length) {
            withStyle(style = SpanStyle(color = Color.White)) {
                append(text.substring(lastIndex))
            }
        }
    }
}

/** "HH:MM:SS.xx" -> seconds (TimeParsing 유틸 사용) */
private fun parseTimeToSeconds(timeString: String): Float = 
    TimeParsing.toSecondsOrZero(timeString)

@ExperimentalGetImage
@OptIn(ExperimentalMirrorMode::class, ExperimentalGetImage::class)
@Composable
fun GamePlayScreen(
    songId: String,
    isPaused: Boolean = false,
    onTogglePause: () -> Unit = {},
    onGameComplete: (GameResultUi) -> Unit = {},
    onGameQuit: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onFrame: ((ImageProxy) -> Unit)? = null,
    judgmentResult: JudgmentResult? = null,
    gamePlayViewModel: GamePlayViewModel? = null,
    playerPositionMs: () -> Long = { 0L }  // ExoPlayer 위치 제공 (기본값)
) {
    val context = LocalContext.current

    // GamePlayViewModel 상태
    val gameUi by (gamePlayViewModel?.ui?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(GameUiState()) })
    val completeUi by (gamePlayViewModel?.complete?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(CompleteUiState()) })
    
    // 🔥 웹소켓 판정 결과 상태 (기존 구조 활용)
    val currentJudgment by (gamePlayViewModel?.currentJudgment?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) })
    
    // 게임 모드 확인
    val gameMode = GameDataManager.currentGameMode.value ?: GameMode.EASY
    
    // 중복 호출 제거: GameDataManager로 이미 채보 데이터 관리됨

    // ExoPlayer
    val player = remember {
        Log.d("GamePlayScreen", "ExoPlayer 인스턴스 생성 시작")
        ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    val stateText = when (playbackState) {
                        Player.STATE_IDLE -> "IDLE"
                        Player.STATE_BUFFERING -> "BUFFERING"
                        Player.STATE_READY -> "READY"
                        Player.STATE_ENDED -> "ENDED"
                        else -> "UNKNOWN($playbackState)"
                    }
                    Log.d("GamePlayScreen", "ExoPlayer 재생 상태 변경: $stateText")
                    
                    // READY 상태가 되면 TimelineViewModel이 정상 작동하는지 확인
                    if (playbackState == Player.STATE_READY) {
                        Log.d("GamePlayScreen", "ExoPlayer READY - TimelineViewModel이 시간 업데이트를 시작해야 함")
                    }
                }
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    Log.d("GamePlayScreen", "ExoPlayer isPlaying 변경: $isPlaying")
                }
                override fun onPlayerError(error: PlaybackException) {
                    Log.e("GamePlayScreen", "ExoPlayer 오류: ${error.message}", error)
                }
                override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                    Log.d("GamePlayScreen", "ExoPlayer MediaItem 전환: ${mediaItem?.localConfiguration?.uri}")
                }
                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int
                ) {
                    Log.d("GamePlayScreen", "ExoPlayer 위치 불연속: ${oldPosition.positionMs}ms -> ${newPosition.positionMs}ms")
                }
            })
        }.also { Log.d("GamePlayScreen", "ExoPlayer 인스턴스 생성 완료") }
    }

    var isScreenVisible by remember { mutableStateOf(true) }

    // Timeline
    val timelineViewModel = remember(player) { TimelineViewModel(player) }
    val tick: TimelineTick? by timelineViewModel.ticks.collectAsState()

    // MediaPipe - 게임 모드에 따라 다른 업로더 사용
    val buffer = remember { DynamicLandmarkBuffer() }
    val resultHandler = remember { 
        LandmarkResultHandler(
            buffer = buffer,
            onLandmarks = { pose, left, right ->
                // 🔥 하드 모드일 때 ViewModel에 랜드마크 결과 전달
                if (gameMode == GameMode.HARD) {
                    gamePlayViewModel?.onLandmarks(pose, left, right)
                }
            }
        )
    }
    
    // 🔥 게임 모드에 따른 업로더 선택
    val uploader = when (gameMode) {
        GameMode.EASY -> WordWindowUploader(buffer, "http://j13a602.p.ssafy.io/api/v1/game/rhythm/play", null)
        GameMode.HARD -> null // 웹소켓은 ViewModel에서 처리
        else -> null
    }
    
    val mediaPipeCamera = remember { 
        GamePlayCamera(resultHandler, uploader ?: WordWindowUploader(buffer, "http://j13a602.p.ssafy.io/api/v1/game/rhythm/play", null))
    }

    LaunchedEffect(Unit) {
        Log.d("GamePlayScreen", "MediaPipe 초기화 시작")
        mediaPipeCamera.init(context)
        Log.d("GamePlayScreen", "MediaPipe 초기화 완료")
    }
    
    // 🔥 게임 시작 시 플레이어 위치 제공자 설정
    LaunchedEffect(gamePlayViewModel) {
        val totalWords = 10 // TODO: 실제 총 단어 수로 설정
        // ExoPlayer의 실제 위치를 사용하도록 수정
        val actualPlayerPositionMs: () -> Long = { 
            val position = player.currentPosition
            if (position == C.TIME_UNSET) 0L else position
        }
        gamePlayViewModel?.startGame(songId, totalWords, gameMode, actualPlayerPositionMs)
    }
    
    // 🔥 하드 모드일 때 리듬 수집기에 프레임 데이터 전달
    LaunchedEffect(gameMode, tick?.positionMs) {
        if (gameMode == GameMode.HARD && tick?.isPlaying == true) {
            val currentMs = tick?.positionMs ?: 0L
            // 300ms 주기로 프레임 수집 (실제로는 MediaPipe에서 처리)
            // TODO: MediaPipe 결과를 리듬 수집기에 전달하는 로직 추가
        }
    }

    // Pause→Resume AC 측정
    var resumeWall by remember { mutableStateOf<Long?>(null) }
    var lastLogged by remember { mutableStateOf<Long?>(null) }
    fun logFirstTickErrorIfNeeded(t: TimelineTick) {
        val r = resumeWall ?: return
        val elapsedSinceResume = t.wallClockMs - r
        val errorMs = kotlin.math.abs(t.positionMs - elapsedSinceResume)
        if (lastLogged != r) {
            Log.d("AC_CHECK", "First tick error = ${errorMs}ms  (<=15ms 목표)")
            lastLogged = r
        }
    }

    val bg = GameTheme.Colors.DarkBackground
    val card = GameTheme.Colors.DarkCard
    val progress = GameTheme.Colors.Progress
    val greenBorder = GameTheme.Colors.GreenBorder

    val currentSong by GameDataManager.currentSong.collectAsState()
    val currentGameMode by GameDataManager.currentGameMode.collectAsState()
    val gameProgressState by GameDataManager.gameProgress.collectAsState()
    
    // 현재 모드에 따른 업로더 선택
    val currentUploader = when (currentGameMode) {
        GameMode.EASY -> uploader
        GameMode.HARD -> uploader // TODO: websocketUploader로 변경
        null -> uploader // 기본값
        else -> uploader // 기본값
    }

    // 곡 선택 및 게임 초기화
    LaunchedEffect(songId) {
        Log.d("GamePlayScreen", "🎵 게임 초기화 시작: songId=$songId")
        
        val song = GameDataManager.getSongById(songId)
        Log.d("GamePlayScreen", "곡 조회 결과: ${song?.title ?: "null"}")
        
        if (song != null) {
            Log.d("GamePlayScreen", "곡 선택: ${song.title}")
            GameDataManager.selectSong(song)
            
            // 게임 시작 (섹션 데이터 로드)
            Log.d("GamePlayScreen", "게임 시작 - 섹션 데이터 로드...")
            GameDataManager.startGame()

            // GamePlayViewModel 초기화
            gamePlayViewModel?.let { vm ->
                Log.d("GamePlayScreen", "채보 데이터 로드 시작...")
                val sections = GameDataManager.getSongSections(songId)
                Log.d("GamePlayScreen", "채보 데이터 로드 완료: ${sections.size}개 섹션")
                sections.forEach { section ->
                    Log.d("GamePlayScreen", "섹션 ${section.id}: '${section.text}' (${section.startTime}s~${section.endTime}s)")
                }

                if (sections.isEmpty()) {
                    Log.e("GamePlayScreen", "⚠️ 채보 데이터가 비어있습니다!")
                } else {
                    Log.d("GamePlayScreen", "✅ 채보 데이터 로드 성공, 게임 시작")
                }

                // ExoPlayer의 실제 위치를 사용하도록 수정
                val actualPlayerPositionMs: () -> Long = { 
                    val position = player.currentPosition
                    if (position == C.TIME_UNSET) 0L else position
                }
                vm.startGame(songId, totalWords = sections.size, mode = gameMode, playerPositionMs = actualPlayerPositionMs)
            } ?: Log.e("GamePlayScreen", "GamePlayViewModel이 null입니다!")
        } else {
            Log.e("GamePlayScreen", "songId에 해당하는 곡 없음: $songId")
        }
    }

    // ExoPlayer 준비/재생
    LaunchedEffect(player, songId, currentSong, isScreenVisible) {
        if (!isScreenVisible) {
            Log.d("GamePlayScreen", "화면이 보이지 않음, ExoPlayer 준비 건너뜀")
            return@LaunchedEffect
        }
        if (currentSong == null) {
            Log.d("GamePlayScreen", "현재 곡이 null, ExoPlayer 준비 건너뜀")
            return@LaunchedEffect
        }

        val song = currentSong
        Log.d("GamePlayScreen", "ExoPlayer 준비 시작: songId=$songId, currentSong=${song?.title}")
        
        if (player.mediaItemCount == 0) {
            Log.d("GamePlayScreen", "음악 URL 로드 시작: ${song?.id}")
            val audioUrl = GameDataManager.getMusicUrl(song?.id ?: "")
            if (audioUrl.isNullOrEmpty()) {
                Log.e("GamePlayScreen", "음악 URL 로드 실패: ${song?.id}")
                return@LaunchedEffect
            }
            Log.d("GamePlayScreen", "음악 URL 로드 성공: $audioUrl")
            
            Log.d("GamePlayScreen", "ExoPlayer MediaItem 설정 시작")
            player.setMediaItem(MediaItem.fromUri(audioUrl))
            Log.d("GamePlayScreen", "ExoPlayer prepare() 호출")
            player.prepare()
            
            Log.d("GamePlayScreen", "ExoPlayer 재생 상태 확인: isPaused=$isPaused")
            if (!isPaused) {
                Log.d("GamePlayScreen", "ExoPlayer play() 호출")
                player.play()
            } else {
                Log.d("GamePlayScreen", "일시정지 상태이므로 재생하지 않음")
            }
        } else {
            Log.d("GamePlayScreen", "ExoPlayer에 이미 MediaItem이 있음: ${player.mediaItemCount}개")
        }
        
        Log.d("GamePlayScreen", "TimelineViewModel 시작")
        timelineViewModel.start()
        
        // ExoPlayer 상태 로그
        Log.d("GamePlayScreen", "ExoPlayer 최종 상태: mediaItemCount=${player.mediaItemCount}, isPlaying=${player.isPlaying}, playbackState=${player.playbackState}")
    }

    // 재생/일시정지 토글 반영
    LaunchedEffect(isPaused, isScreenVisible) {
        if (!isScreenVisible) return@LaunchedEffect
        if (player.mediaItemCount == 0) return@LaunchedEffect
        if (isPaused) player.pause() else player.play()
    }

    // 첫 틱 오차 로깅
    LaunchedEffect(tick?.isPlaying) {
        val t = tick ?: return@LaunchedEffect
        if (t.isPlaying) logFirstTickErrorIfNeeded(t)
    }

    // 현재 시간(초) - ExoPlayer의 실제 재생 위치 사용 (더 정확한 계산)
    val gameTime = remember(player.currentPosition) {
        val positionMs = player.currentPosition
        if (positionMs == C.TIME_UNSET) 0f else (positionMs / 1000f).coerceAtLeast(0f)
    }
    
    // 디버깅: tick 상태 로그
    LaunchedEffect(tick) {
        Log.d("GamePlayScreen", "Tick 상태: positionMs=${tick?.positionMs}, isPlaying=${tick?.isPlaying}, gameTime=${gameTime}s, playerPosition=${player.currentPosition}ms")
    }
    
    // 수동 테스트 코드 제거됨 - ExoPlayer가 정상 작동함
    
    // 중복 제거: 아래 LaunchedEffect에서 통합 처리

    // 수어 타이밍 업로드 트리거
    LaunchedEffect(tick?.positionMs, gameProgressState, isScreenVisible) {
        if (!isScreenVisible) return@LaunchedEffect
        val currentMs = tick?.positionMs ?: return@LaunchedEffect
        val progress = gameProgressState ?: return@LaunchedEffect

        val currentSection = progress.sections.getOrNull(progress.currentSectionIndex) ?: return@LaunchedEffect
        currentSection.correctInfo.forEach { correctInfo ->
            val actionStartTime = (parseTimeToSeconds(correctInfo.actionStartedAt) * 1000).toLong()
            val actionEndTime = (parseTimeToSeconds(correctInfo.actionEndedAt) * 1000).toLong()

            if (currentMs in actionStartTime until (actionStartTime + 100)) {
                // 수어 타이밍 시작 시 버퍼 상태 로그
                Log.d("GamePlayScreen", "수어 타이밍 시작: segment=${currentSection.id}, range=${correctInfo.correctStartedIndex}~${correctInfo.correctEndedIndex}")
                buffer.logBufferDetails()
            }
            if (currentMs in actionEndTime until (actionEndTime + 100)) {
                // 수어 타이밍 종료 시 버퍼 상태 로그
                Log.d("GamePlayScreen", "수어 타이밍 종료: ${currentSection.text}")
                Log.d("GamePlayScreen", "버퍼 상태: ${buffer.getBufferInfo()}")
            }
        }
    }

    // 진행/완료 체크 - ExoPlayer의 실제 곡 길이 사용
    val totalTime = remember(currentSong, player) {
        // ExoPlayer에서 실제 곡 길이 가져오기
        val durationMs = player.duration
        if (durationMs != C.TIME_UNSET && durationMs > 0) {
            val durationSeconds = durationMs / 1000f
            Log.d("GamePlayScreen", "ExoPlayer 실제 곡 길이: ${durationSeconds}s (${durationMs}ms)")
            durationSeconds
        } else {
            // ExoPlayer 길이를 못 가져온 경우 곡 정보 사용
            currentSong?.durationText?.let {
                try {
                    val parts = it.split(":")
                    val calculatedTime = when (parts.size) {
                        2 -> (parts[0].toInt() * 60 + parts[1].toInt()).toFloat()
                        3 -> (parts[0].toInt() * 3600 + parts[1].toInt() * 60 + parts[2].toInt()).toFloat()
                        else -> 200f
                    }
                    Log.d("GamePlayScreen", "곡 정보 기반 총 시간: ${calculatedTime}s (durationText: $it)")
                    calculatedTime
                } catch (_: Exception) { 
                    Log.d("GamePlayScreen", "곡 정보 파싱 실패, 기본값 사용: 200s")
                    200f 
                }
            } ?: run {
                Log.d("GamePlayScreen", "곡 정보 없음, 기본값 사용: 200s")
                200f
            }
        }
    }

    // 게임 완료 상태 추적
    var isGameFinished by remember { mutableStateOf(false) }
    
    // 게임 진행 상태 업데이트 및 완료 체크 (통합)
    LaunchedEffect(gameTime, totalTime, isScreenVisible) {
        if (!isScreenVisible) return@LaunchedEffect
        
        // 게임 진행 상태 업데이트
        GameDataManager.updateGameProgress(gameTime)
        
        // 디버깅 로그 추가
        Log.d("GamePlayScreen", "게임 시간 체크: gameTime=${gameTime}s, totalTime=${totalTime}s")
        
        // 이미 완료된 경우 중복 호출 방지
        if (isGameFinished) {
            Log.d("GamePlayScreen", "게임 이미 완료됨 - 중복 호출 방지")
            return@LaunchedEffect
        }
        
        // 게임 완료 조건: ExoPlayer 재생 완료를 우선 확인
        val isPlayerFinished = player.playbackState == Player.STATE_ENDED
        val isTimeFinished = gameTime >= totalTime && totalTime > 0 && gameTime > 1.0f
        
        if (isPlayerFinished || isTimeFinished) {
            isGameFinished = true
            Log.d("GamePlayScreen", "게임 완료: ${if (isPlayerFinished) "ExoPlayer 재생 완료" else "시간 조건 만족"} (gameTime=${gameTime}s, totalTime=${totalTime}s)")
            gamePlayViewModel?.finishGameAndPost()
        }
    }
    
    // 게임 완료 상태 감지 (새로운 API 사용)
    LaunchedEffect(completeUi.submitted) {
        if (completeUi.submitted) {
            // ViewModel에서 계산된 결과를 사용하여 게임 완료 처리
            val gameResult = GameDataManager.createGameResult(
                songId = songId,
                score = gameUi.score,
                correctCount = gameUi.correctCount,
                missCount = gameUi.missCount,
                maxCombo = gameUi.maxCombo,
                missWords = gameUi.missWords
            )
            GameDataManager.saveGameResult(gameResult)
            onGameComplete(gameResult)
        }
    }

    val songTitle = currentSong?.title ?: "곡을 선택해주세요"
    
    // 섹션 데이터를 직접 가져와서 사용
    val sections = remember { mutableStateOf<List<SongSection>>(emptyList()) }
    
    // 섹션 데이터 로드
    LaunchedEffect(songId) {
        try {
            val loadedSections = GameDataManager.getSongSections(songId)
            sections.value = loadedSections
            Log.d("GamePlayScreen", "섹션 데이터 로드 완료: ${loadedSections.size}개")
        } catch (e: kotlinx.coroutines.CancellationException) {
            Log.d("GamePlayScreen", "코루틴 취소됨 - 섹션 데이터 로드 중단")
            // CancellationException은 정상적인 생명주기 동작이므로 로그만 출력
        } catch (e: Exception) {
            Log.e("GamePlayScreen", "섹션 데이터 로드 실패", e)
        }
    }
    
    // 현재 섹션 인덱스 계산
    val currentSectionIndex = remember { mutableStateOf(0) }
    
    // 시간에 따라 현재 섹션 인덱스 업데이트
    LaunchedEffect(gameTime, sections.value) {
        if (sections.value.isNotEmpty()) {
            val newIndex = sections.value.indexOfFirst { section ->
                gameTime >= section.startTime && gameTime < section.endTime
            }
            if (newIndex >= 0 && newIndex != currentSectionIndex.value) {
                currentSectionIndex.value = newIndex
                Log.d("GamePlayScreen", "섹션 인덱스 업데이트: $newIndex (시간: ${gameTime}s)")
            }
        }
    }
    
    val songProgress = gameProgressState ?: SongProgress(
        songId = songId,
        currentTime = gameTime,
        totalTime = totalTime,
        currentSectionIndex = currentSectionIndex.value,
        sections = sections.value
    )
    
    // 디버깅을 위한 로그
    LaunchedEffect(songProgress.currentSectionIndex, gameTime) {
        val currentSection = songProgress.sections.getOrNull(songProgress.currentSectionIndex)
        Log.d("GamePlayScreen", "현재 섹션: ${songProgress.currentSectionIndex}, 시간: ${gameTime}s, 가사: '${currentSection?.text}'")
        Log.d("GamePlayScreen", "총 섹션 수: ${songProgress.sections.size}")
        
        // 섹션별 시간 정보 로그
        if (songProgress.sections.isEmpty()) {
            Log.w("GamePlayScreen", "⚠️ 섹션 데이터가 비어있습니다!")
        } else {
            songProgress.sections.forEachIndexed { index, section ->
                val isCurrent = index == songProgress.currentSectionIndex
                Log.d("GamePlayScreen", "섹션[$index]: '${section.text}' (${section.startTime}s~${section.endTime}s) ${if (isCurrent) "← 현재" else ""}")
            }
        }
    }

    // 해제
    DisposableEffect(Unit) {
        isScreenVisible = true
        onDispose {
            isScreenVisible = false
            runCatching { mediaPipeCamera.release() }
            runCatching { timelineViewModel.stop() }
            runCatching { player.release() }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = bg) {
        if (isScreenVisible) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // 게임 배경 제거 - 가사 영역에만 파도 효과 적용
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Top bar
                    TopBarSection(
                        title = songTitle,
                        currentTime = songProgress.currentTime,
                        totalDuration = songProgress.totalTime,
                        isPaused = !(tick?.isPlaying ?: false),
                        onTogglePause = onTogglePause,
                        gameMode = currentGameMode
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    // 전체 진행바 - Modern 컴포넌트 사용 (제목 바로 밑으로 이동)
                    GameProgressBar(
                        progress = if (songProgress.totalTime > 0f) songProgress.currentTime / songProgress.totalTime else 0f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    

                    // 게임 상태 표시 (점수, 등급, 콤보) - Modern 컴포넌트 사용 (임시 주석 처리)
                    /*
                    GameScoreCard(
                        score = gameUi.score,
                        grade = if (gameUi.grade.isNotEmpty()) gameUi.grade else "S",
                        maxCombo = gameUi.maxCombo
                    )
                    */
                    

                    Spacer(Modifier.height(24.dp))

                    // Camera area - 실제 카메라 프리뷰 복원 (높이 1.5배 증가)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F2E)),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CameraPreview(
                                modifier = Modifier.fillMaxSize(),
                                lensFacing = CameraSelector.LENS_FACING_FRONT,
                                enableAnalysis = true,
                                onFrame = { imageProxy -> 
                                    mediaPipeCamera.analyzer.analyze(imageProxy)
                                }
                            )
                            judgmentResult?.let { JudgmentOverlay(result = it) }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Lyrics area - GameDataManager의 채보 데이터 사용 (Modern 컴포넌트)
                    val currentSection = songProgress.sections.getOrNull(songProgress.currentSectionIndex)
                    
                    // 디버깅 로그 추가
                    LaunchedEffect(songProgress.sections.size, songProgress.currentSectionIndex) {
                        Log.d("GamePlayScreen", "채보 데이터: ${songProgress.sections.size}개, 현재 인덱스: ${songProgress.currentSectionIndex}")
                        if (songProgress.sections.isNotEmpty()) {
                            Log.d("GamePlayScreen", "첫 번째 섹션: '${songProgress.sections[0].text}' (${songProgress.sections[0].startTime}s~${songProgress.sections[0].endTime}s)")
                        }
                        currentSection?.let {
                            Log.d("GamePlayScreen", "현재 섹션: '${it.text}' (${it.startTime}s~${it.endTime}s)")
                        } ?: Log.w("GamePlayScreen", "현재 섹션이 null입니다!")
                    }
                    
                    // 가사 3소절 데이터 준비
                    val previousSection = currentSection?.let { current ->
                        songProgress.sections
                            .filter { it.startTime < current.startTime }
                            .maxByOrNull { it.startTime }
                    }
                    
                    val nextSection = currentSection?.let { current ->
                        songProgress.sections
                            .filter { it.startTime > current.startTime }
                            .minByOrNull { it.startTime }
                    }
                    
                    // 현재 가사 진행률 계산 (간단한 버전)
                    val lyricProgress = currentSection?.let { current ->
                        val sectionDuration = current.endTime - current.startTime
                        if (sectionDuration > 0) {
                            val elapsed = songProgress.currentTime - current.startTime
                            (elapsed / sectionDuration).coerceIn(0f, 1f)
                        } else 0f
                    } ?: 0f
                    
                    // 가사 영역 - API 연동된 실제 가사 표시 (파도 효과 포함, 높이 조정)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp) // 높이를 줄여서 종료 버튼 공간 확보
                    ) {
                    // 파도 효과 배경 (콤보에 따른 색상 변화)
                    GameBackground(
                        modifier = Modifier.fillMaxSize(),
                        isPlaying = tick?.isPlaying ?: false,
                        combo = gameUi.combo
                    )
                        
                        // 가사 카드 (투명도 조정하여 파동 효과가 보이도록)
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            colors = CardDefaults.cardColors(containerColor = Color(0x801A1F2E)), // 투명도 50%로 조정
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                            // 가사 그룹 (중앙)
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // 이전 가사
                                Text(
                                    text = previousSection?.text ?: "",
                                    color = Color(0xFF9AA3B2),
                                    fontSize = 17.sp, // 18.sp -> 17.sp로 감소
                                    textAlign = TextAlign.Center,
                                    maxLines = 2, // 여러 줄 표시 허용
                                    overflow = TextOverflow.Ellipsis
                                )
                                
                                Spacer(Modifier.height(6.dp))
                                
                                // 현재 가사 (메인) - 수어 하이라이팅 적용
                                val currentHighlights = getCurrentSignHighlight(currentSection, songProgress.currentTime, currentSectionIndex.value, currentGameMode ?: GameMode.EASY)
                                val highlightedText = if (currentSection != null) {
                                    createHighlightedLyrics(currentSection.text, currentHighlights)
                                } else {
                                    buildAnnotatedString {
                                        withStyle(style = SpanStyle(color = Color.White)) {
                                            append("가사를 불러오는 중...")
                                        }
                                    }
                                }
                                
                                Text(
                                    text = highlightedText,
                                    fontSize = 23.sp, // 24.sp -> 23.sp로 감소
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 3, // 여러 줄 표시 허용 (현재 가사는 더 중요하므로)
                                    overflow = TextOverflow.Ellipsis
                                )
                                
                                Spacer(Modifier.height(6.dp))
                                
                                // 다음 가사
                                Text(
                                    text = nextSection?.text ?: "",
                                    color = Color(0xFF6B7280),
                                    fontSize = 17.sp, // 18.sp -> 17.sp로 감소
                                    textAlign = TextAlign.Center,
                                    maxLines = 2, // 여러 줄 표시 허용
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            // 진행률 표시 (하단)
                            LinearProgressIndicator(
                                progress = { lyricProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = Color(0xFF4CAF50),
                                trackColor = Color(0xFF2A2F3E)
                            )
                        }
                    }
                    
                    }
                    

                    // 게임 완료 결과 전송 상태 표시 (최소화)
                    if (completeUi.submitting) {
                        Spacer(Modifier.height(8.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp), // 높이 제한
                            colors = CardDefaults.cardColors(containerColor = card),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color(0xFF4CAF50)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "전송 중...",
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    
                    if (completeUi.submitError != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFF5A5A)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "에러: ${completeUi.submitError}",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    
                    if (completeUi.submitted && completeUi.isBestRecord) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD700)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "🏆 개인 최고 기록 갱신!",
                                color = Color.Black,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )
                        }
                    }
                    
                    if (completeUi.submitted && !completeUi.isBestRecord) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "기록이 저장되었습니다.",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // 종료 버튼 (하단 가운데)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = onGameQuit,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5A5A)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "종료",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "종료",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                }

                
                // 게임 오버레이 효과들
                GameComboAura(
                    combo = gameUi.combo, 
                    modifier = Modifier.align(Alignment.Center)
                )
                
                // 🔥 하드 모드일 때만 웹소켓 판정 결과 표시 (기존 GameJudgmentToast 활용)
                if (gameMode == GameMode.HARD) {
                    GameJudgmentToast(
                        result = currentJudgment,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                // 기존 HTTP 판정 결과도 유지 (Easy 모드용)
                GameJudgmentToast(
                    result = judgmentResult, 
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
