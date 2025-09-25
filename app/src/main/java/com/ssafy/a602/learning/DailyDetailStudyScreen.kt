@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.media3.common.util.UnstableApi::class   // ⬅ 추가
)

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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.launch
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.common.util.Util
import androidx.media3.common.PlaybackException
import android.util.Log

/* ──────────────────────────────────────────────────────────────────────────
 * 상단 탭 전환용 간단 enum (학습/퀴즈)
 *  - 퀴즈로 바꿀 때 onStartQuiz(day) 콜백을 호출하여 상위 내비게이션 처리
 * ────────────────────────────────────────────────────────────────────────── */
private enum class StudyMode { LEARN, QUIZ }

/* ──────────────────────────────────────────────────────────────────────────
 * 메인 화면 Composable
 *  - 진입 시/파라미터 변경 시 ViewModel.load(day) → 서버에서 데이터 수신
 *  - uiState(로딩/성공/에러)에 따라 분기 렌더링
 * ────────────────────────────────────────────────────────────────────────── */
@Composable
fun DailyDetailStudyScreen(
    day: Int,
    onBack: () -> Unit = {},
    onStartQuiz: (day: Int) -> Unit = {},
    viewModel: DailyDetailStudyViewModel = hiltViewModel()
) {
    // 배경 그라데이션
    val bg = Brush.verticalGradient(listOf(Color(0xFFEFFAF2), Color.White))

    // ViewModel의 상태 스트림을 Compose에서 구독
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 리스트 스크롤/코루틴 스코프 준비 (선택 단어로 스크롤 맞출 때 사용)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // 화면 진입/Day 변경 시 데이터 로드 트리거
    LaunchedEffect(day) { viewModel.load(day) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(horizontal = 16.dp)
    ) {
        Column(Modifier.fillMaxSize()) {

            /* ── 상단바 (뒤로가기 + Day + 모드 토글) ── */
            TopBarWithMode(
                day = day,
                mode = StudyMode.LEARN, // 기본은 학습 모드
                onModeChange = { mode ->
                    // 사용자가 "퀴즈 모드" 탭을 누르면 상위에서 화면 전환
                    if (mode == StudyMode.QUIZ) onStartQuiz(day)
                },
                onBack = onBack
            )

            Spacer(Modifier.height(12.dp))

            /* ── 상태에 따라 본문 렌더 ── */
            when (val s = uiState) {
                DailyDetailUiState.Loading -> {
                    // 로딩 인디케이터 중앙 표시
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is DailyDetailUiState.Error -> {
                    // 오류 메시지 + 다시 시도
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(s.message, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.load(day) }) { Text("다시 시도") }
                    }
                }

                is DailyDetailUiState.Success -> {
                    // 서버에서 내려준 당일 단어/영상 목록
                    val items = s.items

                    // 선택된 인덱스(현재 보여줄 단어). 목록이 바뀌면 0으로 초기화
                    var selectedIndex by remember(items) { mutableStateOf(0) }

                    if (items.isNotEmpty()) {
                        val current = items[selectedIndex]

                        /* ── 현재 단어 카드 ── */
                        CurrentWordCard(word = current.word)
                        Spacer(Modifier.height(12.dp))

                        /* ── 영상 섹션
                         *  - 자동재생 금지(사용자가 재생 버튼 클릭해야 시작)
                         *  - 이전/다음 버튼으로 선택 인덱스 변경
                         *  - 리스트도 해당 아이템 위치로 스크롤 맞춤
                         * ──────────────────────────────────────────────── */
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

                        /* ── 학습 목록(좌측 리스트)
                         *  - 항목을 탭하면 해당 단어로 전환(자동재생 X)
                         *  - 선택 항목은 색상과 두께로 강조
                         * ──────────────────────────────────────────────── */
                        StudyListSection(
                            day = day,
                            items = items,
                            selectedIndex = selectedIndex,
                            onSelect = { idx ->
                                selectedIndex = idx
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

/* ──────────────────────────────────────────────────────────────────────────
 * 상단바 + 모드 토글
 *  - 좌측 ← : 뒤로가기
 *  - 우측 토글 : 학습/퀴즈 전환(퀴즈 선택 시 onModeChange 호출)
 * ────────────────────────────────────────────────────────────────────────── */
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
        // 간단한 텍스트 ← 버튼 (아이콘으로 교체 가능)
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

        // 좌/우 2분할 토글
        SegmentedTwoToggle(
            left = "학습 모드",
            right = "퀴즈 모드",
            selectedLeft = (mode == StudyMode.LEARN),
            onChange = { onModeChange(if (it) StudyMode.LEARN else StudyMode.QUIZ) }
        )
    }
}

/* ──────────────────────────────────────────────────────────────────────────
 * 2분할 토글 버튼 (왼쪽=학습, 오른쪽=퀴즈)
 * ────────────────────────────────────────────────────────────────────────── */
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

/* ──────────────────────────────────────────────────────────────────────────
 * 현재 단어 카드 (큰 글씨 + 보조문구)
 * ────────────────────────────────────────────────────────────────────────── */
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = word,
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = green,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 30.sp
                ),
                modifier = Modifier.fillMaxWidth(),  // ✅ 폭 채우기
                textAlign = TextAlign.Start
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

/* ──────────────────────────────────────────────────────────────────────────
 * 영상 섹션
 *  - videoUrl == null/blank 이면 "영상 없음" 대체 박스
 *  - Player는 자동재생 금지 (사용자 클릭 시 play)
 *  - 페이지 표기 + 이전/다음 네비게이션
 * ────────────────────────────────────────────────────────────────────────── */
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

/* ──────────────────────────────────────────────────────────────────────────
 * ExoPlayer: 오버레이 클릭 시에만 재생
 *  - playWhenReady=false로 자동재생 차단
 *  - PlayerView는 controller/버퍼링 표시 세팅
 *  - remember(url)로 URL 변경 시 새로운 플레이어 생성
 *  - onDispose에서 release (메모리/리소스 누수 방지)
 * ────────────────────────────────────────────────────────────────────────── */
@OptIn(UnstableApi::class) // ProgressiveMediaSource 등 일부 API가 Unstable로 표시됩니다.
@Composable
private fun VideoPlayerManualPlay(url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // 1) HTTP DataSource 커스터마이즈: Redirect 허용 + UA + 타임아웃
    val httpFactory = remember(url) {
        DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)   // ★ 핵심: http→https 등 허용
            .setUserAgent(Util.getUserAgent(context, "A602")) // UA 지정(선택)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)
    }

    // 2) DataSource/MediaSource 구성
    val dataSourceFactory = remember(url) {
        DefaultDataSource.Factory(context, httpFactory)
    }
    val mediaSource = remember(url) {
        ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(url))
    }

    // 3) Player 생성
    val exoplayer = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = false // 자동재생 금지
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var playbackState by remember { mutableStateOf(Player.STATE_IDLE) }

    DisposableEffect(exoplayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(state: Int) { playbackState = state } // ★ 추가
            override fun onPlayerError(error: PlaybackException) {
                Log.e("Player", "playback error: ${error.errorCodeName}", error)
            }
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
                    player = exoplayer
                    useController = false
                    setControllerShowTimeoutMs(2000)
                    // 버퍼링 인디케이터 켜기 (필드 접근 말고 setter/속성 쓰기)
//                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    // 선택) 버퍼링 스피너를 아예 숨기고 싶다면 아래 줄로 변경
                     setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                }
            },
            update = { it.player = exoplayer },
            modifier = Modifier.matchParentSize()
        )

        // ▶/↻ 오버레이
        val ended = playbackState == Player.STATE_ENDED
        if (!isPlaying) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { if (ended) exoplayer.seekTo(0)
                        exoplayer.playWhenReady = true
                        exoplayer.play() },
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

/* ──────────────────────────────────────────────────────────────────────────
 * 좌측 학습 목록 리스트
 *  - 선택 항목은 배경/테두리로 강조
 *  - 항목 탭 시 onSelect(index)로 현재 단어 전환 (자동재생은 하지 않음)
 * ────────────────────────────────────────────────────────────────────────── */
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
                            .clickable { onSelect(index) } // 선택만, 자동재생 X
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

/* ──────────────────────────────────────────────────────────────────────────
 * 유틸: 특정 아이템을 화면 중앙 근처로 부드럽게 스크롤
 * ────────────────────────────────────────────────────────────────────────── */
private suspend fun LazyListState.centerOnItem(index: Int) {
    animateScrollToItem(index, scrollOffset = 0)
}
