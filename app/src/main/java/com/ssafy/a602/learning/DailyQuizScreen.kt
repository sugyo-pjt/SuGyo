@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.ssafy.a602.learning

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/* ───────────────────────── 내부 전용 유틸/모델 ───────────────────────── */

// "안녕하세요,안녕하십니까" -> "안녕하세요"
private fun primaryLabel(raw: String): String =
    raw.split(',', '·', '/', ';').firstOrNull()?.trim().orEmpty()

private data class QuizQuestion(
    val videoUrl: String?,
    val correct: String,
    val options: List<String>
)

/** 캐시 목록 → 퀴즈 문항 생성 (그날 학습 개수만큼 / 기본 4지선다) */
private fun buildQuestionsFromCache(
    cache: List<LearningMemCache.Item>,
    count: Int = cache.size,
    optionsPerQuestion: Int = 4
): List<QuizQuestion> {
    if (cache.isEmpty()) return emptyList()

    // 대표어 기준 중복 제거 (동의어 묶음 처리)
    val unique = LinkedHashMap<String, LearningMemCache.Item>() // key = 대표어
    cache.forEach { item ->
        val key = primaryLabel(item.word)
        if (key.isNotBlank() && !unique.containsKey(key)) {
            unique[key] = item.copy(word = key) // 캐시도 대표어로 정규화
        }
    }
    val items = unique.values.toList()
    if (items.size < 2) return emptyList() // 보기 최소 2개 필요

    val qCount = minOf(count, items.size)
    val optCount = optionsPerQuestion.coerceIn(2, items.size)

    val pool = items.shuffled()
    return (0 until qCount).map { idx ->
        val correct = pool[idx]
        val distractors = items
            .filter { it.word != correct.word }
            .shuffled()
            .take(optCount - 1)
            .map { it.word }

        val options = (distractors + correct.word).distinct().shuffled()
        QuizQuestion(
            videoUrl = correct.videoUrl,
            correct = correct.word,
            options = options
        )
    }
}

/* ───────────────────────── 메인 퀴즈 화면 ───────────────────────── */

@Composable
fun DailyQuizScreen(
    day: Int,
    onBack: () -> Unit = {},
    onGoStudy: (Int) -> Unit = {},
    onGoRoadmap: () -> Unit = {}
) {
    val bg = Brush.verticalGradient(listOf(Color(0xFFEFFAF2), Color.White))
    val scope = rememberCoroutineScope()

    // 학습 화면에서 저장해둔 캐시 읽기
    val cached = remember(day) { LearningMemCache.get(day) }

    if (cached.isNullOrEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("퀴즈 데이터를 찾을 수 없어요.", color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { onGoStudy(day) }) { Text("학습 화면으로 돌아가기") }
            }
        }
        return
    }

    // 퀴즈 상태
    val questions = remember(cached) {
        buildQuestionsFromCache(cached, count = cached.size, optionsPerQuestion = 4)
    }
    var index by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf<String?>(null) }
    var showResult by remember { mutableStateOf(false) }
    var resultCorrect by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }
    var finished by remember { mutableStateOf(false) }

    // 점수 저장 상태(데모)
    var submitInProgress by remember { mutableStateOf(false) }
    var submitOk by remember { mutableStateOf<Boolean?>(null) }

    val current = questions.getOrNull(index)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(horizontal = 16.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            // 상단바
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "←",
                    fontSize = 20.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onBack() }
                        .padding(4.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Day $day",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.weight(1f))
                SegmentedTwoToggleQuiz(
                    left = "학습 모드",
                    right = "퀴즈 모드",
                    selectedLeft = false,
                    onChange = { left -> if (left) onGoStudy(day) }
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "${minOf(index + 1, questions.size)}/${questions.size}",
                color = Color(0xFF6B7280),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(8.dp))

            // 본문 카드
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xF2FFFFFF)),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Day $day 퀴즈",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("다음 수어가 나타내는 단어를 선택하세요", color = Color(0xFF6B7280))
                    Spacer(Modifier.height(12.dp))

                    // 문제(영상/플레이스홀더)
                    if (current?.videoUrl != null) {
                        VideoPlayerManualPlayQuiz(url = current.videoUrl)
                    } else {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFE5F4EA)),
                            contentAlignment = Alignment.Center
                        ) { Text("수어 동작", color = Color(0xFF16A34A)) }
                    }

                    Spacer(Modifier.height(12.dp))

                    // 보기
                    val options = current?.options ?: emptyList()
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(options.size) { i ->
                            val opt = options[i]
                            val selectedNow = opt == selected
                            val border = if (selectedNow) Color(0xFF1D4ED8) else Color.Transparent
                            val bgOpt = if (selectedNow) Color(0xFFEFF4FF) else Color(0xFFF5F7F9)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(bgOpt)
                                    .border(2.dp, border, RoundedCornerShape(12.dp))
                                    .clickable { selected = opt }
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                Text(
                                    text = opt,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (selectedNow) FontWeight.SemiBold else FontWeight.Normal
                                    ),
                                    color = if (selectedNow) Color(0xFF1D4ED8) else Color(0xFF111827)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // 제출
                    Button(
                        onClick = {
                            current ?: return@Button
                            val ok = selected == current.correct
                            resultCorrect = ok
                            if (ok) score += 1
                            showResult = true
                        },
                        enabled = selected != null,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) { Text("제출하기") }
                }
            }
        }

        // 정답/오답 다이얼로그
        if (showResult && !finished && current != null) {
            AnswerDialog(
                correct = resultCorrect,
                correctWord = current.correct,
                onNext = {
                    showResult = false
                    selected = null
                    if (index + 1 < questions.size) {
                        index += 1
                    } else {
                        finished = true
                        submitInProgress = true
                        submitOk = null
                        scope.launch {
                            val ok = try {
                                submitClientScore(day, score, total = questions.size)
                            } catch (_: Throwable) {
                                false
                            }
                            submitOk = ok
                            submitInProgress = false
                        }
                    }
                }
            )
        }

        // 완료 다이얼로그
        if (finished) {
            FinishDialog(
                day = day,
                score = score,
                total = questions.size,
                onRetry = {
                    index = 0
                    selected = null
                    showResult = false
                    resultCorrect = false
                    score = 0
                    finished = false
                    submitInProgress = false
                    submitOk = null
                },
                onGoRoadmap = onGoRoadmap
            )

            if (submitInProgress) {
                Text(
                    "결과 저장 중…",
                    color = Color(0xFF6B7280),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
                )
            } else {
                submitOk?.let { ok ->
                    Text(
                        if (ok) "결과 저장 완료" else "저장 실패(네트워크 확인)",
                        color = if (ok) Color(0xFF16A34A) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
                    )
                }
            }
        }
    }
}

/* ───────── 다이얼로그/토글 UI ───────── */

@Composable
private fun AnswerDialog(
    correct: Boolean,
    correctWord: String,
    onNext: () -> Unit
) {
    val bg = if (correct) Color(0xFFE8FFF1) else Color(0xFFFFF1F2)
    val iconBg = if (correct) Color(0xFF16A34A) else Color(0xFFEF4444)
    val title = if (correct) "정답입니다!" else "오답입니다!"
    val sub = if (correct) "잘했습니다" else "정답: $correctWord"

    AlertDialog(
        onDismissRequest = onNext,
        confirmButton = {
            Button(onClick = onNext, shape = RoundedCornerShape(12.dp)) { Text("다음 →") }
        },
        title = null,
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(bg)
                    .padding(horizontal = 18.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (correct) "✓" else "✕", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(4.dp))
                Text(sub, color = Color(0xFF6B7280))
            }
        },
        containerColor = Color.Transparent
    )
}

@Composable
private fun FinishDialog(
    day: Int,
    score: Int,
    total: Int,
    onRetry: () -> Unit,
    onGoRoadmap: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = null,
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E)),
                    contentAlignment = Alignment.Center
                ) { Text("🏆", fontSize = 24.sp) }

                Spacer(Modifier.height(10.dp))
                Text("Day $day 완료!", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "$score / $total",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF16A34A)
                )
                Spacer(Modifier.height(4.dp))
                Text("정답 개수를 확인해보세요", color = Color(0xFF6B7280))

                Spacer(Modifier.height(14.dp))
                Button(onClick = onRetry, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("퀴즈 다시하기")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onGoRoadmap, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("로드맵으로 돌아가기")
                }
            }
        },
        confirmButton = {},
        containerColor = Color.Transparent
    )
}

@Composable
private fun SegmentedTwoToggleQuiz(
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

/* ───────────────────── 퀴즈용 ExoPlayer (학습 화면과 같은 HTTP 설정) ───────────────────── */

@Composable
private fun VideoPlayerManualPlayQuiz(url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // HTTP DataSource: 리다이렉트/UA/타임아웃 설정 (sldict의 http/redirect 대응)
    val httpFactory = remember(url) {
        DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent(Util.getUserAgent(context, "A602"))
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)
    }
    val dataSourceFactory = remember(url) { DefaultDataSource.Factory(context, httpFactory) }
    val mediaSource = remember(url) {
        ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(url))
    }

    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = false // 자동재생 X
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var playbackState by remember { mutableStateOf(Player.STATE_IDLE) }

    DisposableEffect(player) {
        val l = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(state: Int) { playbackState = state }
            override fun onPlayerError(error: PlaybackException) {
                Log.e("QuizPlayer", "playback error: ${error.errorCodeName}", error)
            }
        }
        player.addListener(l)
        onDispose { player.removeListener(l); player.release() }
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
                    this.player = player
                    useController = true
                    setControllerShowTimeoutMs(2000)
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            update = { it.player = player },
            modifier = Modifier.matchParentSize()
        )

        val ended = playbackState == Player.STATE_ENDED
        if (!isPlaying) {
            Box(
                Modifier
                    .matchParentSize()
                    .clickable {
                        if (ended) player.seekTo(0)
                        player.playWhenReady = true
                        player.play()
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.size(64.dp).clip(CircleShape).background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text("▶", color = Color(0xFF22C55E), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/* ───────── 서버 전송 (Demo) ───────── */
private suspend fun submitClientScore(
    day: Int,
    score: Int,
    total: Int
): Boolean {
    // TODO: Retrofit/ktor로 교체
    delay(400)
    return true
}
