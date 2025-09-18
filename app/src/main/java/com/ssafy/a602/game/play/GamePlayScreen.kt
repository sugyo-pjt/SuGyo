package com.ssafy.a602.game.play

import android.os.SystemClock
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ExperimentalMirrorMode
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

/* ========== Utility Functions ========== */

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
    gamePlayViewModel: GamePlayViewModel? = null
) {
    val context = LocalContext.current

    // GamePlayViewModel 상태
    val gameUi by (gamePlayViewModel?.ui?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(GameUiState()) })
    val completeUi by (gamePlayViewModel?.complete?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(CompleteUiState()) })
    
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

    // MediaPipe
    val buffer = remember { DynamicLandmarkBuffer() }
    val resultHandler = remember { LandmarkResultHandler(buffer) }
    val uploader = remember { WordWindowUploader(buffer, endpoint = "https://your.api/landmarks/upload") }
    val mediaPipeCamera = remember { GamePlayCamera(resultHandler, uploader) }

    LaunchedEffect(Unit) {
        Log.d("GamePlayScreen", "MediaPipe 초기화 시작")
        mediaPipeCamera.init(context)
        Log.d("GamePlayScreen", "MediaPipe 초기화 완료")
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

    var showPauseButton by remember { mutableStateOf(false) }
    val bg = GameTheme.Colors.DarkBackground
    val card = GameTheme.Colors.DarkCard
    val progress = GameTheme.Colors.Progress
    val greenBorder = GameTheme.Colors.GreenBorder

    val currentSong by GameDataManager.currentSong.collectAsState()
    val gameProgressState by GameDataManager.gameProgress.collectAsState()

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

                vm.startGame(songId, totalWords = sections.size)
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

    // 현재 시간(초)
    val gameTime = (tick?.positionMs ?: 0L) / 1000f
    
    // 디버깅: tick 상태 로그
    LaunchedEffect(tick) {
        Log.d("GamePlayScreen", "Tick 상태: positionMs=${tick?.positionMs}, isPlaying=${tick?.isPlaying}, gameTime=${gameTime}s")
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
                uploader.onWord(
                    actionStartMs = actionStartTime,
                    actionEndMs = actionEndTime,
                    segment = currentSection.id.toInt(),
                    correctStartedIndex = correctInfo.correctStartedIndex,
                    correctEndedIndex = correctInfo.correctEndedIndex,
                    musicId = songId.toInt()
                )
                
                // 수어 타이밍 시작 시 버퍼 상태 로그
                Log.d("GamePlayScreen", "수어 타이밍 시작: segment=${currentSection.id}, range=${correctInfo.correctStartedIndex}~${correctInfo.correctEndedIndex}")
                buffer.logBufferDetails()
            }
            if (currentMs in actionEndTime until (actionEndTime + 100)) {
                uploader.onActionEnd()
                
                // 수어 타이밍 종료 시 버퍼 상태 로그
                Log.d("GamePlayScreen", "수어 타이밍 종료: ${currentSection.text}")
                Log.d("GamePlayScreen", "버퍼 상태: ${buffer.getBufferInfo()}")
            }
        }
    }

    // 진행/완료 체크 - 실제 채보 데이터의 마지막 섹션 종료 시간 사용
    val totalTime = remember(currentSong, gameProgressState?.sections?.size) {
        // 채보 데이터가 있으면 마지막 섹션의 종료 시간 사용
        gameProgressState?.sections?.lastOrNull()?.endTime?.let { lastSectionEndTime ->
            Log.d("GamePlayScreen", "채보 기반 총 시간: ${lastSectionEndTime}s")
            lastSectionEndTime
        } ?: run {
            // 채보 데이터가 없으면 곡 정보의 durationText 사용
            currentSong?.durationText?.let {
                try {
                    val parts = it.split(":")
                    val calculatedTime = when (parts.size) {
                        2 -> {
                            // MM:SS 형식
                            (parts[0].toInt() * 60 + parts[1].toInt()).toFloat()
                        }
                        3 -> {
                            // HH:MM:SS 형식
                            (parts[0].toInt() * 3600 + parts[1].toInt() * 60 + parts[2].toInt()).toFloat()
                        }
                        else -> 200f // 기본값
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

    // 게임 진행 상태 업데이트 및 완료 체크 (통합)
    LaunchedEffect(gameTime, totalTime, isScreenVisible) {
        if (!isScreenVisible) return@LaunchedEffect
        
        // 게임 진행 상태 업데이트
        GameDataManager.updateGameProgress(gameTime)
        
        // 디버깅 로그 추가
        Log.d("GamePlayScreen", "게임 시간 체크: gameTime=${gameTime}s, totalTime=${totalTime}s")
        
        // 게임 완료 조건: 게임 시간이 총 시간을 초과하고, 총 시간이 0보다 크며, 게임 시간이 1초 이상일 때
        if (gameTime >= totalTime && totalTime > 0 && gameTime > 1.0f) {
            Log.d("GamePlayScreen", "게임 완료 조건 만족: gameTime=${gameTime}s >= totalTime=${totalTime}s")
            // GamePlayViewModel을 사용하여 게임 완료 처리 (새로운 API 사용)
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
    val songProgress = gameProgressState ?: SongProgress(
        songId = songId,
        currentTime = gameTime,
        totalTime = totalTime,
        currentSectionIndex = 0,
        sections = emptyList()
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
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showPauseButton = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // Top bar
                    TopBarSection(
                        title = songTitle,
                        currentTime = songProgress.currentTime,
                        totalDuration = songProgress.totalTime,
                        isPaused = !(tick?.isPlaying ?: false),
                        onTogglePause = onTogglePause,
                        onOpenSettings = { showPauseButton = !showPauseButton },
                        showPauseButton = showPauseButton
                    )
                    
                    // 게임 상태 표시 (점수, 등급, 콤보)
                    if (gameUi.score > 0 || gameUi.grade.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = card),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "Score",
                                        color = Color(0xFF9AA3B2),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        "${gameUi.score}",
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "Grade",
                                        color = Color(0xFF9AA3B2),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        gameUi.grade,
                                        color = Color(0xFFFFD700),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "Max Combo",
                                        color = Color(0xFF9AA3B2),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        "${gameUi.maxCombo}",
                                        color = Color(0xFF4CAF50),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // 전체 진행바
                    LinearProgressIndicator(
                        progress = { if (songProgress.totalTime > 0f) songProgress.currentTime / songProgress.totalTime else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        trackColor = Color(0x33212535),
                        color = progress
                    )

                    Spacer(Modifier.height(16.dp))

                    // Camera area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(card)
                            .border(3.dp, greenBorder, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CameraPreview(
                            modifier = Modifier.fillMaxSize(),
                            lensFacing = CameraSelector.LENS_FACING_FRONT,
                            enableAnalysis = true,
                            onFrame = { imageProxy -> mediaPipeCamera.analyzer.analyze(imageProxy) }
                        )
                        judgmentResult?.let { JudgmentOverlay(result = it) }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Lyrics area - GameDataManager의 채보 데이터 사용
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
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = card),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (currentSection != null) {
                                // 현재 섹션 정보 로그
                                Log.d("GamePlayScreen", "현재 섹션 표시: '${currentSection.text}', correctInfo: ${currentSection.correctInfo.size}개")
                                currentSection.correctInfo.forEachIndexed { index, correct ->
                                    Log.d("GamePlayScreen", "  correct[$index]: 인덱스 ${correct.correctStartedIndex}~${correct.correctEndedIndex}")
                                }
                                
                                val previousSection = songProgress.sections
                                    .filter { it.startTime < currentSection.startTime }
                                    .maxByOrNull { it.startTime }
                                previousSection?.let { prev ->
                                    Text(
                                        prev.text,
                                        color = Color(0xFF9AA3B2),
                                        style = MaterialTheme.typography.labelLarge,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(10.dp))
                                }

                                val nextSection = songProgress.sections
                                    .filter { it.startTime > currentSection.startTime }
                                    .minByOrNull { it.startTime }

                                val highlightedText = buildAnnotatedString {
                                    val text = currentSection.text
                                    val correctInfo = currentSection.correctInfo
                                    Log.d("GamePlayScreen", "가사 하이라이팅: '$text', correctInfo: ${correctInfo.size}개")
                                    
                                    if (correctInfo.isNotEmpty()) {
                                        val first = correctInfo.first()
                                        val startIndex = first.correctStartedIndex
                                        val endIndex = first.correctEndedIndex
                                        Log.d("GamePlayScreen", "하이라이트 범위: $startIndex~$endIndex (텍스트 길이: ${text.length})")
                                        
                                        if (startIndex in 0..text.length && endIndex in 0..text.length && startIndex < endIndex) {
                                            if (startIndex > 0) {
                                                withStyle(SpanStyle(color = Color(0xFFE7ECF3))) {
                                                    append(text.substring(0, startIndex))
                                                }
                                            }
                                            withStyle(SpanStyle(color = Color(0xFFFF4444), fontWeight = FontWeight.Bold)) {
                                                append(text.substring(startIndex, endIndex))
                                            }
                                            if (endIndex < text.length) {
                                                withStyle(SpanStyle(color = Color(0xFFE7ECF3))) {
                                                    append(text.substring(endIndex))
                                                }
                                            }
                                            Log.d("GamePlayScreen", "하이라이트 적용 완료: '${text.substring(startIndex, endIndex)}'")
                                        } else {
                                            Log.w("GamePlayScreen", "하이라이트 범위 오류: startIndex=$startIndex, endIndex=$endIndex, textLength=${text.length}")
                                            withStyle(SpanStyle(color = Color(0xFFE7ECF3))) { append(text) }
                                        }
                                    } else {
                                        Log.d("GamePlayScreen", "correctInfo가 비어있음, 일반 텍스트 표시")
                                        withStyle(SpanStyle(color = Color(0xFFE7ECF3))) { append(text) }
                                    }
                                }

                                Text(
                                    text = highlightedText,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )

                                nextSection?.let { next ->
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        next.text,
                                        color = Color(0xFF6B7280),
                                        style = MaterialTheme.typography.labelMedium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                
                                // 디버깅용 텍스트 제거됨
                            } else {
                                Text(
                                    "곡을 준비하고 있습니다",
                                    color = Color(0xFF9AA3B2),
                                    style = MaterialTheme.typography.labelLarge,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "잠시만 기다려주세요",
                                    color = Color(0xFFE7ECF3),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    
                    // 게임 완료 결과 전송 상태 표시
                    if (completeUi.submitting) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = card),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color(0xFF4CAF50)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "게임 결과를 전송하고 있습니다...",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
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

                    // Bottom button
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

                // 드롭다운 메뉴 오버레이 (설정 버튼 아래)
                if (showPauseButton) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-16).dp, y = 40.dp)
                            .width(140.dp)
                            .clickable { },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2329)),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Button(
                                onClick = {
                                    if (player.isPlaying) {
                                        player.pause()
                                    } else {
                                        player.playWhenReady = true
                                        if (player.playbackState == Player.STATE_IDLE) player.prepare()
                                        player.play()
                                        resumeWall = SystemClock.elapsedRealtime()
                                    }
                                    onTogglePause()
                                    showPauseButton = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (player.isPlaying) Color(0xFFFFA726) else Color(0xFF4CAF50)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                            ) {
                                Icon(
                                    if (player.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (player.isPlaying) "일시정지" else "재생",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    if (player.isPlaying) "일시정지" else "재생",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
