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
import com.ssafy.a602.game.play.input.DynamicLandmarkBuffer
import com.ssafy.a602.game.play.input.LandmarkResultHandler
import com.ssafy.a602.game.play.input.WordWindowUploader
import com.ssafy.a602.game.result.GameResultUi
import com.ssafy.a602.game.time.TimelineTick
import com.ssafy.a602.game.time.TimelineViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/* ========== Utility Functions ========== */

/** "HH:MM:SS.xx" -> seconds */
private fun parseTimeToSeconds(timeString: String): Float = try {
    val parts = timeString.split(":")
    val hours = parts[0].toInt()
    val minutes = parts[1].toInt()
    val secondsWithMs = parts[2].toFloat()
    (hours * 3600 + minutes * 60 + secondsWithMs)
} catch (_: Exception) { 0f }

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

    // ExoPlayer
    val player = remember {
        Log.d("GamePlayScreen", "ExoPlayer 인스턴스 생성 시작")
        ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    Log.d("GamePlayScreen", "재생 상태: $playbackState")
                }
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    Log.d("GamePlayScreen", "isPlaying: $isPlaying")
                }
                override fun onPlayerError(error: PlaybackException) {
                    Log.e("GamePlayScreen", "ExoPlayer 오류: ${error.message}", error)
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
        val song = GameDataManager.getSongById(songId)
        if (song != null) {
            GameDataManager.selectSong(song)
            // GamePlayViewModel 초기화
            gamePlayViewModel?.let { vm ->
                val sections = GameDataManager.getSongSections(songId)
                vm.startGame(songId, totalWords = sections.size)
            }
        } else {
            Log.e("GamePlayScreen", "songId에 해당하는 곡 없음: $songId")
        }
    }

    // ExoPlayer 준비/재생
    LaunchedEffect(player, songId, currentSong, isScreenVisible) {
        if (!isScreenVisible) return@LaunchedEffect
        if (currentSong == null) return@LaunchedEffect

        if (player.mediaItemCount == 0) {
            val audioUrl = GameDataManager.getMusicUrl(currentSong!!.id)
            if (audioUrl == null) {
                Log.e("GamePlayScreen", "음악 URL 로드 실패: ${currentSong!!.id}")
                return@LaunchedEffect
            }
            player.setMediaItem(MediaItem.fromUri(audioUrl))
            player.prepare()
            if (!isPaused) player.play()
        }
        timelineViewModel.start()
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

    // 진행/완료 체크
    val totalTime = remember(currentSong) {
        currentSong?.durationText?.let {
            try {
                val parts = it.split(":")
                when (parts.size) {
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
            } catch (_: Exception) { 200f }
        } ?: 200f
    }

    LaunchedEffect(gameTime, totalTime, isScreenVisible) {
        if (!isScreenVisible) return@LaunchedEffect
        GameDataManager.updateGameProgress(gameTime)
        if (gameTime >= totalTime && totalTime > 0) {
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

                    // Lyrics area
                    val currentSection = songProgress.sections.getOrNull(songProgress.currentSectionIndex)
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
                                    if (correctInfo.isNotEmpty()) {
                                        val first = correctInfo.first()
                                        val startIndex = first.correctStartedIndex
                                        val endIndex = first.correctEndedIndex
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
                                        } else {
                                            withStyle(SpanStyle(color = Color(0xFFE7ECF3))) { append(text) }
                                        }
                                    } else {
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
