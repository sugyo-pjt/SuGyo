package com.ssafy.a602.search

// ViewModel/Lifecycle

// ExoPlayer
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView

// URL 유효성 검사 함수
private fun isValidVideoUrl(url: String): Boolean {
    return try {
        val uri = java.net.URI(url)
        uri.scheme?.let { scheme ->
            scheme.equals("http", ignoreCase = true) || 
            scheme.equals("https", ignoreCase = true)
        } ?: false
    } catch (e: Exception) {
        false
    }
}



/* ----------------------------- 스크린 ------------------------------ */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchDetailScreen(
    wordId: Long,
    onBack: () -> Unit = {},
    onPlayVideo: () -> Unit = {}
) {
    val vm: SearchDetailViewModel = hiltViewModel()
    
    // wordId 설정
    LaunchedEffect(wordId) {
        vm.setWordId(wordId)
    }
    val bg = Brush.verticalGradient(
        listOf(Color(0xFFF2F6FF), Color(0xFFF8FAFF), Color.White)
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("단어 상세", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { inner ->
        when {
            vm.loading -> {
                Box(Modifier.fillMaxSize().padding(inner)) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            }
            vm.error != null -> {
                Box(Modifier.fillMaxSize().padding(inner)) {
                    Text(
                        "오류가 발생했습니다: ${vm.error}",
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            vm.ui != null -> {
                val ui = vm.ui!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bg)
                        .padding(inner)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TitleCard(ui.word)
                    VideoCard(
                        title = "수어 영상",
                        videoUrl = ui.videoUrl,
                        onPlay = onPlayVideo
                    )
                    TextSectionCard(
                        title = "상세 설명",
                        body = ui.description
                    )
                    if (ui.sameMotionWords.isNotEmpty()) {
                        SameMotionSectionCard(
                            title = "다의 수어",
                            words = ui.sameMotionWords
                        )
                    }
                }
            }
        }
    }
}


/* --------------------------- 구성요소들 ---------------------------- */

@Composable
private fun TitleCard(text: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp)),
        color = Color.White,
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun VideoCard(
    title: String,
    videoUrl: String?,
    onPlay: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFEFF2F7)),
                contentAlignment = Alignment.Center
            ) {
                if (!videoUrl.isNullOrBlank() && isValidVideoUrl(videoUrl)) {
                    // ExoPlayer를 사용한 비디오 재생
                    VideoPlayer(videoUrl = videoUrl)
                } else {
                    // 비디오 URL이 없거나 유효하지 않을 때 기본 UI
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "재생",
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (videoUrl.isNullOrBlank()) "영상이 없습니다" else "영상을 재생할 수 없습니다", 
                            color = Color.Gray
                        )
                        if (!videoUrl.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text("URL: $videoUrl", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun VideoPlayer(videoUrl: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var hasError by remember { mutableStateOf(false) }
    
    val exoPlayer = remember(videoUrl) {
        // DefaultHttpDataSource 설정 (리다이렉트 허용)
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Android-App/1.0")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(10000)
            .setReadTimeoutMs(10000)
        
        // MediaSourceFactory 설정
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
        
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(videoUrl))
                playWhenReady = false // 자동 재생 비활성화
                repeatMode = androidx.media3.common.Player.REPEAT_MODE_OFF
                
                // 에러 리스너 추가
                addListener(object : androidx.media3.common.Player.Listener {
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        hasError = true
                    }
                })
            }
    }

    // videoUrl이 변경될 때마다 새로운 미디어 아이템 설정
    LaunchedEffect(videoUrl) {
        hasError = false
        exoPlayer.setMediaItem(MediaItem.fromUri(videoUrl))
        exoPlayer.prepare()
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    if (hasError) {
        // 에러 발생 시 대체 UI
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = "재생",
                modifier = Modifier.size(48.dp),
                tint = Color.Red
            )
            Spacer(Modifier.height(8.dp))
            Text("영상 재생 오류", color = Color.Red)
            Spacer(Modifier.height(4.dp))
            Text("URL: $videoUrl", color = Color.Gray, fontSize = 12.sp)
        }
    } else {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = true
                    setShowBuffering(androidx.media3.ui.PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun TextSectionCard(
    title: String,
    body: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(
                text = body,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Color(0xFF374151)
            )
        }
    }
}

@Composable
private fun SameMotionSectionCard(
    title: String,
    words: List<String>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(
                text = words.joinToString(", "),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                fontSize = 14.sp
            )
        }
    }
}


@Preview(
    name = "Search Detail – Light",
    showBackground = true,
    backgroundColor = 0xFFF6F8FF
)
@Preview(
    name = "Search Detail – Dark",
    showBackground = true,
    backgroundColor = 0xFF000000,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun PreviewSearchDetailScreen() {
    // 필요 시 프로젝트의 MaterialTheme로 교체
    MaterialTheme {
        SearchDetailScreen(
            wordId = 1L,
            onBack = {},
            onPlayVideo = {}
        )
    }
}