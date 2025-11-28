@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.media3.common.util.UnstableApi::class
)

package com.ssafy.a602.learning

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ChevronRight
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.ssafy.a602.learning.api.MusicStudyItem
import com.ssafy.a602.learning.api.MusicStudyListResponse
import com.ssafy.a602.learning.api.StudyApiService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────
// 목록 화면 전용 UiState / ViewModel
// ─────────────────────────────────────────────────────────────
data class SongStudyListUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val items: List<MusicStudyItem> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class SongStudyListViewModel @Inject constructor(
    private val api: StudyApiService
) : androidx.lifecycle.ViewModel() {

    private val _state = MutableStateFlow(SongStudyListUiState())
    val state: StateFlow<SongStudyListUiState> = _state.asStateFlow()

    init { load() }

    fun onQueryChange(q: String) {
        _state.update { it.copy(query = q) }
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val res = api.getMusicStudyList()
                if (res.isSuccessful) {
                    val list = res.body()?.musics ?: emptyList()
                    _state.update { it.copy(isLoading = false, items = list) }
                } else {
                    _state.update {
                        it.copy(isLoading = false, error = "HTTP ${res.code()} ${res.message()}")
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "알 수 없는 오류") }
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────
   노래 학습: 목록 화면
   ✔️ /api/v1/music/study 결과를 뿌리도록 수정
   ───────────────────────────────────────────────────────────── */
@Composable
fun SongStudyListScreen(
    onBack: () -> Unit,
    onOpenDetail: (String, String) -> Unit,  // songId, songTitle
    vm: SongStudyListViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()

    val filtered = remember(state.items, state.query) {
        val q = state.query.trim()
        if (q.isBlank()) state.items
        else state.items.filter {
            it.title.contains(q, ignoreCase = true) || it.singer.contains(q, ignoreCase = true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // 새로운 상단바 디자인 - 그라데이션 배경
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFF8FAFF),
                                Color(0xFFE8F2FF)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack, 
                            contentDescription = "뒤로",
                            tint = Color(0xFF1A1A1A)
                        )
                    }
                    Text(
                        text = "노래 학습",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1A1A1A),
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

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

            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null -> {
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(state.error!!, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = vm::load) { Text("다시 시도") }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(filtered, key = { it.musicId }) { item ->
                            MusicStudyCard(
                                item = item,
                                onClick = { onOpenDetail(item.musicId.toString(), item.title) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicStudyCard(
    item: MusicStudyItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // 학습 아이콘 (앨범 이미지 대신)
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF3B82F6),
                                Color(0xFF8B5CF6)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.School,
                    contentDescription = "학습 아이콘",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF1F2937),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.singer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                
                // 단어 개수만 표시
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF0F9FF)
                ) {
                    Text(
                        text = "단어 ${item.countWord}개",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF0369A1),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // 화살표 아이콘
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = "이동",
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/* ─────────────────────────────────────────────────────────────
   노래 학습: 상세 화면 (기존과 동일 UI)
   ───────────────────────────────────────────────────────────── */
@Composable
fun SongStudyDetailScreen(
    musicId: String,
    songTitle: String,
    onBack: () -> Unit,
    viewModel: SongStudyDetailViewModel = hiltViewModel()
) {
    val bg = Brush.verticalGradient(listOf(Color(0xFFEFFAF2), Color.White))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(musicId, songTitle) { viewModel.load(musicId, songTitle) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxWidth()) {
                TopBarWithSongTitle(
                    songTitle = when (val s = uiState) {
                        is SongStudyDetailUiState.Success -> s.songTitle
                        else -> "노래 학습"
                    },
                    onBack = onBack
                )

                Spacer(Modifier.height(12.dp))

                when (val s = uiState) {
                    SongStudyDetailUiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                            Button(onClick = { viewModel.load(musicId, songTitle) }) { Text("다시 시도") }
                        }
                    }

                    is SongStudyDetailUiState.Success -> {
                        val words = s.words
                        var selectedIndex by remember(words) { mutableStateOf(0) }

                        if (words.isNotEmpty()) {
                            val currentWord = words[selectedIndex]

                            CurrentWordCard(word = currentWord.word)
                            Spacer(Modifier.height(12.dp))

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
            text = "←",
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

/* ─────────────────────────────────────────────────────────────
 * 현재 단어 카드
 * ───────────────────────────────────────────────────────────── */
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

/* ─────────────────────────────────────────────────────────────
 * 영상 섹션 (수동 재생)
 * ───────────────────────────────────────────────────────────── */
@OptIn(UnstableApi::class)
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

/* ─────────────────────────────────────────────────────────────
 * ExoPlayer: 수동 재생
 * ───────────────────────────────────────────────────────────── */
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

/* ─────────────────────────────────────────────────────────────
 * 노래 단어 목록 리스트
 * ───────────────────────────────────────────────────────────── */
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

private suspend fun LazyListState.centerOnItem(index: Int) {
    animateScrollToItem(index, scrollOffset = 0)
}
