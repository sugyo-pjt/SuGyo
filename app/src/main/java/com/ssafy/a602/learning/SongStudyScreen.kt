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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Search
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.ssafy.a602.game.data.GameDataManager
import com.ssafy.a602.game.songs.SongCard      // ✅ 게임 카드 재사용 (룩앤필 통일)
import com.ssafy.a602.game.songs.SongItem
import com.ssafy.a602.game.songs.SongsViewModel
import kotlinx.coroutines.launch
import android.util.Log

/* ─────────────────────────────────────────────────────────────
   노래 학습: 목록 화면 (게임 SongsScreen 과 동일한 톤/스타일)
   - 뒤로가기 + 타이틀은 Row로 구성 (TopAppBar 미사용)
   - 검색창과 카드 스타일은 게임 화면과 동일
   - 카드 클릭 시 onOpenDetail(musicId)
   ───────────────────────────────────────────────────────────── */
@Composable
fun SongStudyListScreen(
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit
) {
    val vm: SongsViewModel = viewModel()
    val state by vm.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFF)) // 게임 화면과 동일 배경
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // 상단: 뒤로가기 + 타이틀 (게임과 같은 타이틀 톤)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로")
                }
                Text(
                    text = "노래 학습",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1A1A1A)
                )
            }

            Spacer(Modifier.height(16.dp))

            // 검색창 (게임과 동일 스타일)
            OutlinedTextField(
                value = state.query,
                onValueChange = vm::onQueryChange,
                placeholder = { Text("곡 검색…", color = Color(0xFF9CA3AF)) },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        tint = Color(0xFF6B7280)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            // 곡 목록
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(state.filtered, key = { it.id }) { song ->
                    // 게임 UI와 완전 동일하게 보여주기 위해 카드 재사용
                    SongCard(song) { onOpenDetail(song.id) }
                }
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────
   노래 학습: 상세 화면
   - 간단 정보 카드들로 구성 (필요 시 영상/가사/구간연습 추가)
   - 목록과 배경/톤 통일
   ───────────────────────────────────────────────────────────── */
@Composable
fun SongStudyDetailScreen(
    musicId: String,
    onBack: () -> Unit,
    viewModel: SongStudyDetailViewModel = hiltViewModel()
) {
    // 배경 그라데이션
    val bg = Brush.verticalGradient(listOf(Color(0xFFEFFAF2), Color.White))

    // ViewModel의 상태 스트림을 Compose에서 구독
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 리스트 스크롤/코루틴 스코프 준비 (선택 단어로 스크롤 맞출 때 사용)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // var song by remember { mutableStateOf<SongItem?>(null) }

    // 간단히 전체 목록에서 찾아오기 (실서비스면 캐시/DI 추천)
    LaunchedEffect(musicId) {
        viewModel.load(musicId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            // .background(Color(0xFFF8FAFF))
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // .padding(horizontal = 16.dp)
        ) {
            Column( Modifier.fillMaxWidth()) {
                // 상단바
                TopBarWithSongTitle(
                    songTitle = when(val s = uiState) {
                        is SongStudyDetailUiState.Success -> s.songTitle
                        else -> "노래 학습"
                    },
                    onBack = onBack
                )
                
                Spacer(Modifier.height(12.dp))

                // 상태에 따라 본문 렌더
                when (val s = uiState) {
                    SongStudyDetailUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is SongStudyDetailUiState.Error -> {
                        Column(
                            Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(s.message, color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { viewModel.load(musicId) }) { Text("다시 시도") }
                        }
                    }
                

                    is SongStudyDetailUiState.Success -> {
                        val words = s.words

                        // 선택된 인덱스
                        var selectedIndex by remember(words) { mutableStateOf(0) }

                        if (words.isNotEmpty()) {
                            val currentWord = words[selectedIndex]

                            // 현재 단어 카드
                            CurrentWordCard(word = currentWord.word)
                            Spacer(Modifier.height(12.dp))

                            // 영상 섹션
                            VideoSection(
                                videoUrl = currentWord.videoUrl,
                                page = selectedIndex + 1,
                                total = words.size,
                                onPrev = {
                                    if (selectedIndex > 0) {
                                        selectedIndex--
                                        scope.launch { listState.centerOnItem(selectedIndex) }
                                    }
                                },
                                onNext = {
                                    if (selectedIndex < words.lastIndex) {
                                        selectedIndex++
                                        scope.launch { listState.centerOnItem(selectedIndex) }
                                    }
                                }
                            )

                            Spacer(Modifier.height(16.dp))

                            // 단어 목록
                            SongWordListSection(
                                songTitle = s.songTitle,
                                words = words,
                                selectedIndex = selectedIndex,
                                onSelect = { idx ->
                                    selectedIndex = idx
                                    scope.launch { listState.centerOnItem(idx) }
                                },
                                listState = listState
                            )
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("학습할 단어가 없습니다.")
                            }
                        }
                    }
                }
            }
        }
    }
}



// --------------------------------------------------------------------------------
// 상단바 + 곡 제목
// --------------------------------------------------------------------------------

@Composable
private fun TopBarWithSongTitle(
    songTitle: String,
    onBack: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        Text(
            text="←",
            fontSize = 20.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { onBack() }
                .padding(4.dp)
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = songTitle,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
    }
}

/* ──────────────────────────────────────────────────────────────────────────
 * 현재 단어 카드 (DailyDetailStudyScreen과 동일)
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
                modifier = Modifier.fillMaxWidth(),
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
 * 영상 섹션 (DailyDetailStudyScreen과 동일)
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
            VideoPlayerManualPlay(url = videoUrl)
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
 * ExoPlayer: 수동 재생 (DailyDetailStudyScreen과 동일)
 * ────────────────────────────────────────────────────────────────────────── */
@OptIn(UnstableApi::class)
@Composable
private fun VideoPlayerManualPlay(url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val httpFactory = remember(url) {
        DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent(Util.getUserAgent(context, "A602"))
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)
    }

    val dataSourceFactory = remember(url) {
        DefaultDataSource.Factory(context, httpFactory)
    }
    val mediaSource = remember(url) {
        ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(url))
    }

    val exoplayer = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = false
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var playbackState by remember { mutableStateOf(Player.STATE_IDLE) }

    DisposableEffect(exoplayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(state: Int) { playbackState = state }
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
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                }
            },
            update = { it.player = exoplayer },
            modifier = Modifier.matchParentSize()
        )

        val ended = playbackState == Player.STATE_ENDED
        if (!isPlaying) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { 
                        if (ended) exoplayer.seekTo(0)
                        exoplayer.playWhenReady = true
                        exoplayer.play() 
                    },
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
 * 노래 단어 목록 리스트 (DailyDetailStudyScreen의 StudyListSection과 동일)
 * ────────────────────────────────────────────────────────────────────────── */
@Composable
private fun SongWordListSection(
    songTitle: String,
    words: List<SongWordItem>,
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
                text = "$songTitle 단어 목록",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(12.dp))

            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(words) { index, wordItem ->
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
                            .clickable { onSelect(index) }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = wordItem.word,
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
 * 유틸: 리스트 중앙 스크롤
 * ────────────────────────────────────────────────────────────────────────── */
private suspend fun LazyListState.centerOnItem(index: Int) {
    animateScrollToItem(index, scrollOffset = 0)
}