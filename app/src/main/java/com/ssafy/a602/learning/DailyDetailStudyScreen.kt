package com.ssafy.a602.learning

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/* ───────────────── 모델/상태(이 파일 안에서만 사용) ───────────────── */
private data class DailyStudyItem(
    val word: String,
    val videoUrl: String? = null
)
private enum class StudyMode { LEARN, QUIZ }
private sealed interface DailyStudyUiState  {
    data object Loading : DailyStudyUiState
    data class Success(val items: List<DailyStudyItem>) : DailyStudyUiState
    data class Error(val throwable: Throwable) : DailyStudyUiState
}

/* ─────── 가짜 백엔드(실제 API로 교체 예정) : day별 더미 데이터 반환 ─────── */
private suspend fun fetchDailyStudyFromServer(day: Int): List<DailyStudyItem> {
    delay(150) // 네트워크 지연 흉내
    // day 값에 따라 다른 데이터 내려주는 척
    return when (day) {
        1 -> listOf(
            DailyStudyItem("안녕하세요", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"),
            DailyStudyItem("나", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"),
            DailyStudyItem("사과"), DailyStudyItem("바나나"), DailyStudyItem("포도")
        )
        2 -> listOf(DailyStudyItem("좋아한다"), DailyStudyItem("싫어한다"), DailyStudyItem("나는 사과를 좋아한다"))
        else -> listOf(DailyStudyItem("샘플 A"), DailyStudyItem("샘플 B"))
    }
}

/* ───────────────────────── 메인 화면 ───────────────────────── */
@Composable
fun DailyDetailStudyScreen(
    day: Int,
    onBack: () -> Unit = {},
    onStartQuiz: (day: Int) -> Unit = {}
) {
    val bg = Brush.verticalGradient(listOf(Color(0xFFEFFAF2), Color.White))

    var uiState by remember { mutableStateOf<DailyStudyUiState >(DailyStudyUiState .Loading) }
    var mode by remember { mutableStateOf(StudyMode.LEARN) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // 화면 진입/Day 변경 시 데이터 로딩
    LaunchedEffect(day) {
        uiState = DailyStudyUiState .Loading
        try {
            val items = fetchDailyStudyFromServer(day)
            uiState = DailyStudyUiState .Success(items)
            if (items.isNotEmpty()) listState.scrollToItem(0) // 첫 단어 보이게
        } catch (t: Throwable) {
            uiState = DailyStudyUiState .Error(t)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(horizontal = 16.dp)
    ) {
        Column(Modifier.fillMaxSize()) {

            /* ── 상단바(뒤로가기 + Day + 모드 토글) ── */
            TopBarWithMode(
                day = day,
                mode = mode,
                onModeChange = {
                    mode = it
                    if (it == StudyMode.QUIZ) onStartQuiz(day)
                },
                onBack = onBack
            )

            Spacer(Modifier.height(12.dp))

            when (val s = uiState) {
                DailyStudyUiState .Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is DailyStudyUiState .Error -> {
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("불러오기에 실패했어요.", color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = {
                            scope.launch {
                                uiState = DailyStudyUiState.Loading
                                try {
                                    uiState = DailyStudyUiState.Success(fetchDailyStudyFromServer(day))
                                } catch (t: Throwable) { uiState = DailyStudyUiState.Error(t) }
                            }
                        }) { Text("다시 시도") }
                    }
                }
                is DailyStudyUiState.Success -> {
                    val items = s.items
                    // 선택 인덱스: 목록이 로드되면 0으로 시작(첫 단어 자동 렌더링)
                    var selectedIndex by remember(items) { mutableStateOf(0) }

                    if (items.isNotEmpty()) {
                        val current = items[selectedIndex]

                        CurrentWordCard(word = current.word)
                        Spacer(Modifier.height(12.dp))

                        // ▶ 자동재생 금지: 사용자가 플레이 영역을 눌러야 재생 시작
                        VideoSection(
                            videoUrl = current.videoUrl,
                            page = selectedIndex + 1,
                            total = items.size,
                            onPrev = {
                                if (selectedIndex > 0) {
                                    selectedIndex--
                                    scope.launch { listState.centerOnItem(selectedIndex) }
                                }
                            },
                            onNext = {
                                if (selectedIndex < items.lastIndex) {
                                    selectedIndex++
                                    scope.launch { listState.centerOnItem(selectedIndex) }
                                }
                            }
                        )

                        Spacer(Modifier.height(16.dp))

                        StudyListSection(
                            day = day,
                            items = items,
                            selectedIndex = selectedIndex,
                            onSelect = { idx ->
                                selectedIndex = idx  // 단어만 변경(자동재생 X)
                                scope.launch { listState.centerOnItem(idx) }
                            },
                            listState = listState
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("학습 항목이 없습니다.")
                        }
                    }
                }
            }
        }
    }
}

/* ───────── 상단바 + 모드 토글 ───────── */
@Composable
private fun TopBarWithMode(
    day: Int,
    mode: StudyMode,
    onModeChange: (StudyMode) -> Unit,
    onBack: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        Text(
            text = "←",
            fontSize = 20.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { onBack() }
                .padding(4.dp)
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = "Day $day",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )

        Spacer(Modifier.weight(1f))

        SegmentedTwoToggle(
            left = "학습 모드",
            right = "퀴즈 모드",
            selectedLeft = (mode == StudyMode.LEARN),
            onChange = { onModeChange(if (it) StudyMode.LEARN else StudyMode.QUIZ) }
        )
    }
}

@Composable
private fun SegmentedTwoToggle(
    left: String,
    right: String,
    selectedLeft: Boolean,
    onChange: (Boolean) -> Unit
) {
    val blue = Color(0xFF1D4ED8)
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .clip(shape)
            .background(Color(0xFFE9EEF9))
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val leftBg = if (selectedLeft) blue else Color.Transparent
        val rightBg = if (!selectedLeft) blue else Color.Transparent
        val leftColor = if (selectedLeft) Color.White else blue
        val rightColor = if (!selectedLeft) Color.White else blue

        Text(
            left,
            color = leftColor,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(leftBg)
                .clickable { onChange(true) }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
        Text(
            right,
            color = rightColor,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(rightBg)
                .clickable { onChange(false) }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

/* ───────── 현재 단어 카드 ───────── */
@Composable
private fun CurrentWordCard(word: String) {
    val green = Color(0xFF16A34A)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xF2FFFFFF)),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = word,
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = green,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 30.sp
                ),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "단어의 수어 동작을 확인해보세요",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B7280)
            )
        }
    }
}

/* ───────── 영상 섹션(수동 재생) ───────── */
@Composable
private fun VideoSection(
    videoUrl: String?,
    page: Int,
    total: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Column {
        if (!videoUrl.isNullOrBlank()) {
            VideoPlayerManualPlay(url = videoUrl) // 자동재생 X, 눌러야 재생
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE5F4EA)),
                contentAlignment = Alignment.Center
            ) {
                Text("영상이 없습니다", color = Color(0xFF22C55E))
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onPrev,
                enabled = page > 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .height(36.dp)
            ) { Text("← 이전") }

            Spacer(Modifier.weight(1f))
            Text("$page / $total", color = Color(0xFF6B7280))
            Spacer(Modifier.weight(1f))

            Button(
                onClick = onNext,
                enabled = page < total,
                shape = RoundedCornerShape(999.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) { Text("다음 →") }
        }
    }
}

/* ───────── ExoPlayer (오버레이 클릭 시에만 재생) ───────── */
@Composable
private fun VideoPlayerManualPlay(url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val exoplayer = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = false // 자동재생 금지
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    DisposableEffect(exoplayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
        }
        exoplayer.addListener(listener)
        onDispose {
            exoplayer.removeListener(listener)
            exoplayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoplayer                // ✅ 그대로
                    useController = true              // ✅ 그대로
                    setControllerShowTimeoutMs(2000)  // ✅ 속성 X, setter 메서드 사용
                    setShowBuffering(
                        PlayerView.SHOW_BUFFERING_WHEN_PLAYING  // ✅ 상수도 PlayerView 소속
                    )
                }
            },
            update = { view -> view.player = exoplayer },      // ✅ 재결합 안전
            modifier = Modifier.matchParentSize()
        )

        if (!isPlaying) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { exoplayer.play() }, // ✅ 사용자가 눌러야 재생
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text("▶", color = Color(0xFF22C55E), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/* ───────── 학습 목록 ───────── */
@Composable
private fun StudyListSection(
    day: Int,
    items: List<DailyStudyItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    listState: LazyListState
) {
    val blue = Color(0xFF1D4ED8)
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF2FFFFFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Day $day 학습 목록",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(12.dp))

            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(items) { index, item ->
                    val selected = index == selectedIndex
                    val borderColor = if (selected) blue else Color.Transparent
                    val bg = if (selected) Color(0xFFEFF4FF) else Color(0xFFF5F7F9)

                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(bg)
                            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
                            .clickable { onSelect(index) } // 선택만(자동재생 X)
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = item.word,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (selected) blue else Color(0xFF111827)
                        )
                    }
                }
            }
        }
    }
}

/* ───────── 유틸: 스크롤 위치 맞추기 ───────── */
private suspend fun LazyListState.centerOnItem(index: Int) {
    animateScrollToItem(index, scrollOffset = 0) // Compose 버전별 파라미터명 주의
}
