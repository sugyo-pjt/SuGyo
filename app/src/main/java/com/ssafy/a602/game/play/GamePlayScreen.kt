package com.ssafy.a602.game

import android.os.SystemClock
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ExperimentalMirrorMode
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.ssafy.a602.game.data.GameDataManager
import com.ssafy.a602.game.play.GamePlayCamera
import com.ssafy.a602.game.play.input.LandmarkBuffer3s
import com.ssafy.a602.game.play.input.LandmarkResultHandler
import com.ssafy.a602.game.play.input.WordWindowUploader
import com.ssafy.a602.game.time.TimelineTick
import com.ssafy.a602.game.time.TimelineViewModel

/* ========== Data Classes ========== */

sealed class JudgmentResult {
    object Perfect : JudgmentResult()
    object Miss : JudgmentResult()
}

data class SongSection(
    val startTime: Float,    // 소절 시작 시간 (초)
    val duration: Float,     // 소절 길이 (초)
    val lyrics: String,      // 해당 소절의 가사
    val highlightRange: IntRange? = null // 하이라이트할 단어 범위
)

data class SongProgress(
    val currentTime: Float,      // 현재 시간 (초)
    val totalDuration: Float,    // 전체 곡 길이 (초)
    val sections: List<SongSection> // 소절별 정보
) {
    // 현재 진행률 계산 (0.0 ~ 1.0)
    val progress: Float
        get() = if (totalDuration > 0) currentTime / totalDuration else 0f
    
    // 현재 소절 찾기
    val currentSection: SongSection?
        get() = sections.find { section ->
            currentTime >= section.startTime && 
            currentTime < section.startTime + section.duration
        }
    
    // 현재 소절 내에서의 진행률 (0.0 ~ 1.0)
    val currentSectionProgress: Float
        get() = currentSection?.let { section ->
            val sectionElapsed = currentTime - section.startTime
            if (section.duration > 0) sectionElapsed / section.duration else 0f
        } ?: 0f
}

/* ========== Screen ========== */

@ExperimentalMirrorMode
@ExperimentalGetImage
@Composable
fun GamePlayScreen(
    songId: String,
    isPaused: Boolean = false,
    onTogglePause: () -> Unit = {},
    onGameComplete: (GameResultUi) -> Unit = {}, // 게임 완료 시 (곡 끝까지 재생)
    onGameQuit: () -> Unit = {}, // 게임 중 종료 버튼 클릭 시
    onOpenSettings: () -> Unit = {},
    onFrame: ((ImageProxy) -> Unit)? = null, // 분석이 필요하면 넘겨서 켤 수 있음
    // 판정 결과 표시
    judgmentResult: JudgmentResult? = null // PERFECT 또는 MISS
) {
    val context = LocalContext.current
    
    // ExoPlayer 인스턴스 생성
    // ExoPlayer는 Google에서 개발한 Android용 미디어 플레이어 라이브러리
    // 오디오/비디오 재생, 스트리밍, 다양한 포맷 지원 등의 기능 제공
    val player = remember {
        Log.d("GamePlayScreen", "ExoPlayer 인스턴스 생성 시작")
        val exoPlayer = ExoPlayer.Builder(context).build()
        
        // ExoPlayer 상태 변화 리스너 추가 (디버깅용)
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.d("GamePlayScreen", "ExoPlayer 재생 상태 변화: $playbackState")
            }
            
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Log.d("GamePlayScreen", "ExoPlayer 재생 중 상태 변화: $isPlaying")
            }
            
            override fun onPlayerError(error: PlaybackException) {
                Log.e("GamePlayScreen", "ExoPlayer 오류: ${error.message}", error)
            }
        })
        
        Log.d("GamePlayScreen", "ExoPlayer 인스턴스 생성 완료")
        exoPlayer
    }
    
    // TimelineViewModel 생성
    // TimelineViewModel은 ExoPlayer의 재생 상태를 관찰하고
    // 게임에 필요한 정확한 타이밍 정보를 제공하는 ViewModel
    val timelineViewModel = remember(player) {
        Log.d("GamePlayScreen", "TimelineViewModel 생성 시작")
        val viewModel = TimelineViewModel(player)
        Log.d("GamePlayScreen", "TimelineViewModel 생성 완료")
        viewModel
    }
    
    // MediaPipe 통합을 위한 의존성 생성
    val buffer = remember { LandmarkBuffer3s() }
    val resultHandler = remember { LandmarkResultHandler(buffer) }
    val uploader = remember { 
        WordWindowUploader(buffer, endpoint = "https://your.api/landmarks/upload") 
    }
    val mediaPipeCamera = remember { GamePlayCamera(resultHandler, uploader) }
    
    // MediaPipe 초기화
    LaunchedEffect(Unit) {
        Log.d("GamePlayScreen", "MediaPipe 초기화 시작")
        mediaPipeCamera.init(context)
        Log.d("GamePlayScreen", "MediaPipe 초기화 완료")
    }

    // 단어 이벤트 트리거 함수
    fun onWordEvent(centerMs: Long, wordId: String) {
        Log.d("GamePlayScreen", "단어 이벤트 트리거: centerMs=$centerMs, wordId=$wordId")
        uploader.onWord(centerMs, wordId)
    }
    
    // 단어 이벤트 트리거 조건 확인 함수
    fun shouldTriggerWordEvent(wordCenterMs: Long, currentMs: Long, toleranceMs: Long = 500L): Boolean {
        return kotlin.math.abs(currentMs - wordCenterMs) <= toleranceMs
    }
    
    // TimelineTick 수집
    // TimelineTick은 ExoPlayer의 현재 재생 위치와 상태를 담고 있는 데이터 클래스
    // Choreographer를 통해 약 16ms마다 업데이트되어 정확한 타이밍 제공
    val tick: TimelineTick? by timelineViewModel.ticks.collectAsState()
    
    // Pause→Resume AC 측정용
    var resumeWall by remember { mutableStateOf<Long?>(null) }
    var lastLogged by remember { mutableStateOf<Long?>(null) }
    
    fun logFirstTickErrorIfNeeded(t: TimelineTick) {
        val r = resumeWall ?: return
        val elapsedSinceResume = t.wallClockMs - r
        val errorMs = kotlin.math.abs(t.positionMs - elapsedSinceResume)
        if (lastLogged != r) {
            android.util.Log.d(
                "AC_CHECK",
                "First tick error = ${errorMs}ms  (<=15ms 목표)"
            )
            lastLogged = r
        }
    }
    
    var showPauseButton by remember { mutableStateOf(false) }
    val bg = GameTheme.Colors.DarkBackground
    val card = GameTheme.Colors.DarkCard
    val progress = GameTheme.Colors.Progress
    val greenBorder = GameTheme.Colors.GreenBorder
    
    // GameDataManager에서 현재 곡과 진행 상태 가져오기
    val currentSong by GameDataManager.currentSong.collectAsState()
    val gameProgress by GameDataManager.gameProgress.collectAsState()
    
    // 디버깅: currentSong 상태 로그
    LaunchedEffect(currentSong) {
        Log.d("GamePlayScreen", "currentSong 상태 변화: ${currentSong?.title ?: "null"}")
    }
    
    // songId를 기반으로 곡 자동 선택
    LaunchedEffect(songId) {
        Log.d("GamePlayScreen", "songId 기반 곡 선택: $songId")
        val song = GameDataManager.getSongById(songId)
        if (song != null) {
            Log.d("GamePlayScreen", "곡 자동 선택: ${song.title}")
            GameDataManager.selectSong(song)
        } else {
            Log.e("GamePlayScreen", "songId에 해당하는 곡을 찾을 수 없습니다: $songId")
        }
    }
    
    // ExoPlayer 초기화 및 미디어 설정
    // LaunchedEffect를 사용하여 player, songId, currentSong이 변경될 때마다 실행
    LaunchedEffect(player, songId, currentSong) {
        Log.d("GamePlayScreen", "ExoPlayer 초기화 LaunchedEffect 시작")
        Log.d("GamePlayScreen", "현재 상태: songId=$songId, currentSong=${currentSong?.title ?: "null"}")
        
        // currentSong이 null인 경우 처리
        if (currentSong == null) {
            Log.w("GamePlayScreen", "currentSong이 null입니다. 곡을 먼저 선택해야 합니다.")
            return@LaunchedEffect
        }
        
        // 미디어 아이템이 설정되지 않은 경우에만 초기화
        if (player.mediaItemCount == 0) {
            currentSong?.let { song ->
                // 선택된 곡의 실제 음악 파일 URL 사용 (API에서 가져온 URL)
                val audioUrl = song.audioUrl ?: throw IllegalStateException("음악 파일 URL이 없습니다. API에서 올바른 URL을 제공해야 합니다.")
                Log.d("GamePlayScreen", "ExoPlayer 미디어 설정: $audioUrl")
                
                // MediaItem 생성: ExoPlayer가 재생할 미디어를 나타내는 객체
                val mediaItem = MediaItem.fromUri(audioUrl)
                
                // ExoPlayer에 미디어 아이템 설정
                player.setMediaItem(mediaItem)
                
                // 미디어 준비: 네트워크에서 로딩, 메타데이터 파싱 등
                player.prepare()
                Log.d("GamePlayScreen", "ExoPlayer 미디어 준비 완료")
                
                // 게임이 일시정지 상태가 아니면 자동으로 재생 시작
                if (!isPaused) {
                    Log.d("GamePlayScreen", "ExoPlayer 자동 재생 시작")
                    player.play()
                } else {
                    Log.d("GamePlayScreen", "ExoPlayer 일시정지 상태로 시작")
                }
            }
        } else {
            Log.d("GamePlayScreen", "ExoPlayer에 이미 미디어가 설정되어 있습니다. mediaItemCount=${player.mediaItemCount}")
        }
        
        // TimelineViewModel 시작: Choreographer를 통한 정확한 타이밍 시작
        Log.d("GamePlayScreen", "TimelineViewModel.start() 호출")
        timelineViewModel.start()
    }
    
    // isPaused 상태에 따라 ExoPlayer 재생/일시정지 제어
    // 게임의 일시정지 상태가 변경될 때마다 ExoPlayer의 재생 상태를 동기화
    LaunchedEffect(isPaused) {
        Log.d("GamePlayScreen", "isPaused 상태 변경: $isPaused")
        
        // 미디어가 로드된 상태에서만 재생/일시정지 제어
        if (player.mediaItemCount > 0) {
            if (isPaused) {
                Log.d("GamePlayScreen", "ExoPlayer 일시정지")
                player.pause() // ExoPlayer 재생 일시정지
            } else {
                Log.d("GamePlayScreen", "ExoPlayer 재생")
                player.play() // ExoPlayer 재생 시작
            }
        } else {
            Log.w("GamePlayScreen", "미디어가 로드되지 않아 재생/일시정지 제어 불가")
        }
    }
    
    // 게임 시작 시 자동 재생 (처음 한 번만)
    // 컴포넌트가 처음 생성될 때 한 번만 실행되는 LaunchedEffect
    LaunchedEffect(Unit) {
        Log.d("GamePlayScreen", "게임 시작 시 자동 재생 체크")
        
        // 미디어가 로드되고 일시정지 상태가 아닌 경우 자동 재생
        if (player.mediaItemCount > 0 && !isPaused) {
            Log.d("GamePlayScreen", "게임 시작 시 자동 재생 실행")
            player.play()
        } else {
            Log.d("GamePlayScreen", "자동 재생 조건 미충족: mediaCount=${player.mediaItemCount}, isPaused=$isPaused")
        }
    }
    
    // 컴포넌트 해제 시 정리
    // DisposableEffect는 컴포넌트가 화면에서 사라질 때 정리 작업을 수행
    DisposableEffect(Unit) {
        onDispose { 
            Log.d("GamePlayScreen", "컴포넌트 해제 시작")
            
            // MediaPipe 리소스 해제
            mediaPipeCamera.release()
            Log.d("GamePlayScreen", "MediaPipe 리소스 해제 완료")
            
            // TimelineViewModel 정지: Choreographer 콜백 제거
            timelineViewModel.stop()
            Log.d("GamePlayScreen", "TimelineViewModel 정지 완료")
            
            // ExoPlayer 리소스 해제: 메모리 누수 방지
            player.release()
            Log.d("GamePlayScreen", "ExoPlayer 리소스 해제 완료")
        }
    }
    
    // 첫 틱 오차 로깅
    LaunchedEffect(tick?.isPlaying) {
        val t = tick ?: return@LaunchedEffect
        if (t.isPlaying) logFirstTickErrorIfNeeded(t)
    }
    
    // ExoPlayer의 현재 위치를 기반으로 게임 시간 업데이트
    // TimelineTick의 positionMs(밀리초)를 초 단위로 변환하여 게임 시간으로 사용
    val gameTime = (tick?.positionMs ?: 0L) / 1000f
    
    // 단어 이벤트 트리거 처리
    // 현재 시간이 단어의 중심 시각에 도달했을 때 MediaPipe 데이터 업로드 트리거
    LaunchedEffect(tick?.positionMs) {
        tick?.let { currentTick ->
            val currentMs = currentTick.positionMs
            // TODO: 실제 단어 타임라인 데이터를 사용하여 단어 이벤트 트리거
            // 예시: 단어의 centerMs와 현재 시간이 ±500ms 내에 있을 때 트리거
            // if (shouldTriggerWordEvent(word.centerMs, currentMs)) {
            //     onWordEvent(word.centerMs, word.wordId)
            // }
        }
    }
    
    // 디버깅용 로그: TimelineTick 상태 변화 모니터링
    LaunchedEffect(tick) {
        tick?.let {
            Log.d("GamePlayScreen", "TimelineTick 업데이트: position=${it.positionMs}ms, isPlaying=${it.isPlaying}")
        }
    }
    
    // 곡의 총 길이 계산 (durationText를 초로 변환)
    val totalDuration = remember(currentSong) {
        currentSong?.durationText?.let { durationText ->
            try {
                val parts = durationText.split(":")
                val minutes = parts[0].toInt()
                val seconds = parts[1].toInt()
                (minutes * 60 + seconds).toFloat()
            } catch (e: Exception) {
                62f // 기본값 (1분 2초)
            }
        } ?: 62f
    }
    
    // ExoPlayer 기반 게임 진행 업데이트
    LaunchedEffect(gameTime, totalDuration) {
        GameDataManager.updateGameProgress(gameTime)
        
        // 게임 완료 체크 (곡의 실제 길이 사용)
        if (gameTime >= totalDuration && totalDuration > 0) {
            // 백엔드에서 계산된 결과를 받아오기
            val gameResult = GameDataManager.createGameResult(
                songId = songId,
                score = 876_420, // TODO: 실제 게임에서 계산된 점수
                correctCount = 65, // TODO: 실제 게임에서 계산된 정답 개수
                missCount = 17, // TODO: 실제 게임에서 계산된 실패 개수
                maxCombo = 27, // TODO: 실제 게임에서 계산된 최대 콤보
                missWords = listOf("함께", "만들어", "기억", "별", "여름밤", "망령") // TODO: 실제 게임에서 수집된 실패한 단어들
            )
            // 게임 결과를 GameDataManager에 저장
            GameDataManager.saveGameResult(gameResult)
            onGameComplete(gameResult)
        }
    }
    
    // 곡이 선택되지 않았으면 기본값 사용
    val songTitle = currentSong?.title ?: "곡을 선택해주세요"
    val songProgress = gameProgress ?: SongProgress(
        currentTime = gameTime,
        totalDuration = totalDuration,
        sections = emptyList()
    )

    Surface(modifier = Modifier.fillMaxSize(), color = bg) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { showPauseButton = false } // 메뉴 외부 클릭 시 닫기
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                /* Top bar */
                TopBarSection(
                    title = songTitle,
                    currentTime = songProgress.currentTime,
                    totalDuration = songProgress.totalDuration,
                    // ExoPlayer의 실제 재생 상태를 UI에 반영
                    // tick?.isPlaying이 true면 재생 중, false면 일시정지 상태
                    isPaused = !(tick?.isPlaying ?: false),
                    onTogglePause = onTogglePause,
                    onOpenSettings = {
                        showPauseButton = !showPauseButton
                    },
                    showPauseButton = showPauseButton
                )

                Spacer(Modifier.height(8.dp))
                
                // 전체 진행바
                // ExoPlayer의 현재 재생 위치를 기반으로 한 곡의 진행률 표시
                LinearProgressIndicator(
                    progress = { songProgress.progress }, // 0.0 ~ 1.0 범위의 진행률
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    trackColor = Color(0x33212535),
                    color = progress
                )

                Spacer(Modifier.height(16.dp))

                /* Camera area */
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(card)
                        .border(3.dp, greenBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // 게임 플레이 - 카메라 프리뷰 표시 (MediaPipe 분석 포함)
                    CameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        lensFacing = CameraSelector.LENS_FACING_FRONT,
                        enableAnalysis = true,
                        onFrame = { imageProxy: androidx.camera.core.ImageProxy -> 
                            mediaPipeCamera.analyzer.analyze(imageProxy) 
                        }
                    )
                    
                    // 판정 결과 오버레이
                    judgmentResult?.let { result ->
                        JudgmentOverlay(result = result)
                    }
                }

                Spacer(Modifier.height(16.dp))

                /* Lyrics area */
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = card),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 현재 소절의 가사 표시
                        songProgress.currentSection?.let { currentSection ->
                            // 이전 소절 가사 (있는 경우)
                            val previousSection = songProgress.sections
                                .filter { it.startTime < currentSection.startTime }
                                .maxByOrNull { it.startTime }
                            
                            previousSection?.let { prev ->
                                Text(
                                    prev.lyrics,
                                    color = Color(0xFF9AA3B2),
                                    style = MaterialTheme.typography.labelLarge,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(10.dp))
                            }
                            
                            // 다음 소절 가사 (미리보기)
                            val nextSection = songProgress.sections
                                .filter { it.startTime > currentSection.startTime }
                                .minByOrNull { it.startTime }
                            
                            // 현재 소절 가사 (하이라이트 포함)
                            val currentLyrics = currentSection.lyrics
                            val body = buildAnnotatedString {
                                currentSection.highlightRange?.let { highlightRange ->
                                    if (highlightRange.first in 0..currentLyrics.lastIndex &&
                                        highlightRange.last in 0..currentLyrics.lastIndex &&
                                        highlightRange.first <= highlightRange.last
                                    ) {
                                        append(currentLyrics.substring(0, highlightRange.first))
                                        withStyle(
                                            SpanStyle(
                                                color = Color(0xFFFF5A5A),
                                                fontWeight = FontWeight.Bold
                                            )
                                        ) {
                                            append(currentLyrics.substring(highlightRange))
                                        }
                                        if (highlightRange.last < currentLyrics.lastIndex) {
                                            append(currentLyrics.substring(highlightRange.last + 1))
                                        }
                                    } else {
                                        append(currentLyrics)
                                    }
                                } ?: run {
                                    append(currentLyrics)
                                }
                            }

                            Text(
                                body,
                                color = Color(0xFFE7ECF3),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            
                            // 다음 소절 미리보기
                            nextSection?.let { next ->
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    next.lyrics,
                                    color = Color(0xFF6B7280),
                                    style = MaterialTheme.typography.labelMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } ?: run {
                            // 소절 정보가 없을 때 기본 메시지
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

                /* Bottom button */
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    contentAlignment = Alignment.Center
                ) {
                    // 둥근 정사각형 종료 버튼
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

            // 드롭다운 메뉴 오버레이 (설정 버튼 바로 아래)
            if (showPauseButton) {
                Card(
                    modifier = Modifier
                        .offset(x = 200.dp, y = 40.dp)
                        .width(140.dp)
                        .clickable { }, // 메뉴 클릭 시 닫히지 않도록
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2329)),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (player.isPlaying) {
                                    player.pause()
                                } else {
                                    player.playWhenReady = true
                                    if (player.playbackState == Player.STATE_IDLE) {
                                        player.prepare()
                                    }
                                    player.play()
                                    // Resume 버튼 누른 시각 기록
                                    resumeWall = SystemClock.elapsedRealtime()
                                }
                                onTogglePause()
                                showPauseButton = false // 메뉴 닫기
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

