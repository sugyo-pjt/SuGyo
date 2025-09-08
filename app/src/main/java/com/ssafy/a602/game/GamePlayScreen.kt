package com.ssafy.a602.game

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.ssafy.a602.game.data.GameDataManager
import java.util.concurrent.Executors

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

@Composable
fun GamePlayScreen(
    isPaused: Boolean = false,
    onTogglePause: () -> Unit = {},
    onEnd: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onFrame: ((ImageProxy) -> Unit)? = null, // 분석이 필요하면 넘겨서 켤 수 있음
    // 판정 결과 표시
    judgmentResult: JudgmentResult? = null // PERFECT 또는 MISS
) {
    var showPauseButton by remember { mutableStateOf(false) }
    val bg = Color(0xFF0D1118)
    val card = Color(0xFF151B24)
    val progress = Color(0xFF8B5CF6)   // 보라 진행바
    val greenBorder = Color(0xFF2BD46D)
    
    // GameDataManager에서 현재 곡과 진행 상태 가져오기
    val currentSong by GameDataManager.currentSong.collectAsState()
    val gameProgress by GameDataManager.gameProgress.collectAsState()
    
    // 곡이 선택되지 않았으면 기본값 사용
    val songTitle = currentSong?.title ?: "곡을 선택해주세요"
    val songProgress = gameProgress ?: SongProgress(
        currentTime = 0f,
        totalDuration = 180f,
        sections = emptyList()
    )

    Surface(modifier = Modifier.fillMaxSize(), color = bg) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .statusBarsPadding()
            ) {
                /* Top bar */
                TopBarSection(
                    title = songTitle,
                    currentTime = songProgress.currentTime,
                    totalDuration = songProgress.totalDuration,
                    isPaused = isPaused,
                    onTogglePause = onTogglePause,
                    onOpenSettings = {
                        showPauseButton = !showPauseButton
                        onOpenSettings()
                    },
                    showPauseButton = showPauseButton
                )

                Spacer(Modifier.height(8.dp))
                
                // 전체 진행바
                LinearProgressIndicator(
                    progress = { songProgress.progress },
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
                    // 게임 플레이 - 카메라 프리뷰 표시
                    CameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        lensFacing = CameraSelector.LENS_FACING_FRONT,
                        enableAnalysis = onFrame != null,
                        onFrame = onFrame
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
                        onClick = onEnd,
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
                        .offset(x = 200.dp, y = 60.dp)
                        .width(140.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2329)),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Button(
                            onClick = onTogglePause,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA726)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            Icon(
                                Icons.Filled.Pause,
                                contentDescription = "일시정지",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "일시정지",
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

/* ========== Small Composables ========== */

@Composable
private fun TopBarSection(
    title: String,
    currentTime: Float,
    totalDuration: Float,
    isPaused: Boolean,
    onTogglePause: () -> Unit,
    onOpenSettings: () -> Unit,
    showPauseButton: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = Color(0xFFE7ECF3),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.weight(1f)
        )
        Text(
            formatClock(currentTime.toInt()),
            color = Color(0xFFE7ECF3),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.width(8.dp))

        // 설정 아이콘
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0x22212535))
        ) {
            Icon(
                Icons.Outlined.Settings,
                contentDescription = "설정",
                tint = Color(0xFFB8C2D6)
            )
        }
    }
}

private fun formatClock(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return "%d:%02d".format(m, s)
}

// camera preview
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    lensFacing: Int = CameraSelector.LENS_FACING_FRONT,
    enableAnalysis: Boolean = false,
    mirrorPreview: Boolean = true, // 미러 ON 기본
    onFrame: ((ImageProxy) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Preview 모드에서는 카메라를 사용하지 않고 플레이스홀더 표시
    val isPreviewMode = context.javaClass.name.contains("Preview") ||
                       context.javaClass.name.contains("ComposeViewAdapter")

    if (isPreviewMode) {
        // Preview 모드에서는 카메라 아이콘만 표시
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Outlined.CameraAlt,
                    contentDescription = "카메라",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "수어 인식 카메라",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        return
    }

    val controller = remember {
        try {
            LifecycleCameraController(context).apply {
                cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()
            }
        } catch (e: Exception) {
            // 카메라 초기화 실패 시 null 반환
            null
        }
    }

    // controller가 null이면 플레이스홀더 표시
    if (controller == null) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Outlined.CameraAlt,
                    contentDescription = "카메라",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "카메라를 사용할 수 없습니다",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        return
    }

    // 1) Executor를 한번만 만들고, 화면 사라질 때 종료
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose { analyzerExecutor.shutdown() }
    }

    // 2) 분석기 on/off를 수명에 맞춰 관리
    DisposableEffect(enableAnalysis, onFrame) {
        if (enableAnalysis && onFrame != null) {
            controller.setImageAnalysisBackpressureStrategy(
                ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
            )
            controller.setImageAnalysisAnalyzer(analyzerExecutor) { image ->
                try {
                    onFrame(image)
                } finally {
                    image.close()
                }
            }
            controller.setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
        } else {
            controller.clearImageAnalysisAnalyzer()
            controller.setEnabledUseCases(0) // 분석 끔 (필요 시 PREVIEW만 켜도 됨)
        }
        onDispose {
            // 해제 시점에 안전하게 클리어
            controller.clearImageAnalysisAnalyzer()
        }
    }

    // 3) 실제 프리뷰 붙이기
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
                // 전면 카메라일 때 거울 효과
                if (lensFacing == CameraSelector.LENS_FACING_FRONT && mirrorPreview) {
                    scaleX = -1f
                }
                controller.bindToLifecycle(lifecycleOwner)
                this.controller = controller
            }
        },
        update = { previewView ->
            // lensFacing 또는 mirrorPreview 바뀌면 반영
            previewView.scaleX =
                if (lensFacing == CameraSelector.LENS_FACING_FRONT && mirrorPreview) -1f else 1f
        }
    )
}

/* ========== Judgment Overlay ========== */

@Composable
private fun JudgmentOverlay(
    result: JudgmentResult
) {
    // Preview에서도 잘 보이도록 단순한 애니메이션으로 변경
    val infiniteTransition = rememberInfiniteTransition(label = "judgment")
    
    // 페이드 인/아웃 애니메이션 (더 빠르게)
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    // 스케일 애니메이션 (더 부드럽게)
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when (result) {
                is JudgmentResult.Perfect -> "PERFECT"
                is JudgmentResult.Miss -> "MISS"
            },
            color = when (result) {
                is JudgmentResult.Perfect -> Color(0xFF3B82F6) // 파란색
                is JudgmentResult.Miss -> Color(0xFFFF5A5A) // 빨간색
            },
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier
                .alpha(alpha)
                .scale(scale)
        )
    }
}

/* ========== Preview ========== */

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    backgroundColor = 0xFF0D1118
)
@Composable
private fun GamePlayScreenPreview() {
    var paused by remember { mutableStateOf(false) }
    var judgment by remember { mutableStateOf<JudgmentResult?>(null) }
    
    // Preview용 더미 데이터 설정
    LaunchedEffect(Unit) {
        val sampleSong = Song(
            id = "way_back_home",
            title = "WAY BACK HOME",
            artist = "SHAUN",
            durationText = "3:14",
            bpm = 120,
            rating = 4.2,
            bestScore = 89650,
            thumbnailRes = null
        )
        GameDataManager.selectSong(sampleSong)
        GameDataManager.startGame()
    }
    
    GamePlayScreen(
        isPaused = paused,
        onTogglePause = { paused = !paused },
        onEnd = {},
        onOpenSettings = {},
        judgmentResult = judgment
    )
}

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    backgroundColor = 0xFF0D1118
)
@Composable
private fun GamePlayScreenPerfectPreview() {
    // Preview용 더미 데이터 설정
    LaunchedEffect(Unit) {
        val sampleSong = Song(
            id = "way_back_home",
            title = "WAY BACK HOME",
            artist = "SHAUN",
            durationText = "3:14",
            bpm = 120,
            rating = 4.2,
            bestScore = 89650,
            thumbnailRes = null
        )
        GameDataManager.selectSong(sampleSong)
        GameDataManager.startGame()
    }
    
    GamePlayScreen(
        isPaused = false,
        onTogglePause = { },
        onEnd = {},
        onOpenSettings = {},
        judgmentResult = JudgmentResult.Perfect
    )
}

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    backgroundColor = 0xFF0D1118
)
@Composable
private fun GamePlayScreenMissPreview() {
    // Preview용 더미 데이터 설정
    LaunchedEffect(Unit) {
        val sampleSong = Song(
            id = "way_back_home",
            title = "WAY BACK HOME",
            artist = "SHAUN",
            durationText = "3:14",
            bpm = 120,
            rating = 4.2,
            bestScore = 89650,
            thumbnailRes = null
        )
        GameDataManager.selectSong(sampleSong)
        GameDataManager.startGame()
    }
    
    GamePlayScreen(
        isPaused = false,
        onTogglePause = { },
        onEnd = {},
        onOpenSettings = {},
        judgmentResult = JudgmentResult.Miss
    )
}

// 간단한 판정 결과만 보여주는 Preview
@Preview(
    showBackground = true,
    widthDp = 200,
    heightDp = 200,
    backgroundColor = 0xFF0D1118
)
@Composable
private fun JudgmentOverlayPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF151B24))
            .border(3.dp, Color(0xFF2BD46D), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "PERFECT",
            color = Color(0xFF3B82F6),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(
    showBackground = true,
    widthDp = 200,
    heightDp = 200,
    backgroundColor = 0xFF0D1118
)
@Composable
private fun JudgmentOverlayMissPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF151B24))
            .border(3.dp, Color(0xFF2BD46D), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "MISS",
            color = Color(0xFFFF5A5A),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
