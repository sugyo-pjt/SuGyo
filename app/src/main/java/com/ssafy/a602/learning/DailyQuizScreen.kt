package com.ssafy.a602.learning

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlin.random.Random

/* ───────────────────────── 내부 전용 모델 ───────────────────────── */
private data class QuizItem(val word: String, val videoUrl: String?)
private data class Question(
    val videoUrl: String?,      // 문제로 보여줄 영상 (없으면 텍스트 카드)
    val correct: String,        // 정답 단어
    val options: List<String>   // 보기들(셔플 완료)
)


// 백엔드에서는 이런형식으로 api보내줘야함.
//{
//    "day": 3,
//    "pool": [
//    { "id": "w001", "word": "안녕하세요", "videoUrl": "https://cdn.example.com/sign/day3/hello.mp4" },
//    { "id": "w002", "word": "나",        "videoUrl": "https://cdn.example.com/sign/day3/me.mp4" },
//    { "id": "w003", "word": "사과",      "videoUrl": null },
//    { "id": "w004", "word": "바나나",    "videoUrl": null },
//    { "id": "w005", "word": "포도",      "videoUrl": null }
//    ],
//    "recommendation": {
//    "questionCount": 10,
//    "optionsPerQuestion": 4
//}
//}
/* ─────────── 가짜 API : Day별 문제 원천 데이터(백엔드 연동시 교체) ─────────── */
private suspend fun fetchQuizPool(day: Int): List<QuizItem> {
    delay(120)
    return when (day) {
        1 -> listOf(
            QuizItem("안녕하세요", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"),
            QuizItem("나",        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"),
            QuizItem("사과", null),
            QuizItem("바나나", null),
            QuizItem("포도", null),
            QuizItem("감사합니다", null),
            QuizItem("죄송합니다", null)
        )
        else -> listOf(
            QuizItem("좋아한다", null),
            QuizItem("싫어한다", null),
            QuizItem("나는 사과를 좋아한다", null),
            QuizItem("배고프다", null)
        )
    }
}

/* ─────────── 문제 생성기 : 원천 목록 → Question 리스트 ─────────── */
private fun buildQuestions(pool: List<QuizItem>, count: Int = 10): List<Question> {
    if (pool.isEmpty()) return emptyList()
    val base = pool.shuffled()
    val qCount = minOf(count, base.size)

    return (0 until qCount).map { idx ->
        val correct = base[idx]
        val others = pool.filter { it.word != correct.word }
            .shuffled(Random).take(3).map { it.word }
        val options = (others + correct.word).shuffled(Random)
        Question(
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

    var questions by remember { mutableStateOf<List<Question>>(emptyList()) }
    var index by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf<String?>(null) }
    var showResult by remember { mutableStateOf(false) }
    var resultCorrect by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }
    var finished by remember { mutableStateOf(false) }

    // 데이터 로딩
    LaunchedEffect(day) {
        val pool = fetchQuizPool(day)
        questions = buildQuestions(pool)
        index = 0
        selected = null
        showResult = false
        score = 0
        finished = false
    }

    val current = questions.getOrNull(index)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(horizontal = 16.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            // ── 상단 바(뒤로가기 + 진행표시 + 모드 토글) ──
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
                Text("Day $day", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.weight(1f))
                // 학습/퀴즈 세그먼트 — 왼쪽(학습) 누르면 학습 화면으로 이동
                SegmentedTwoToggle(
                    left = "학습 모드",
                    right = "퀴즈 모드",
                    selectedLeft = false,
                    onChange = { left -> if (left) onGoStudy(day) }
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "${minOf(index + 1, questions.size)}/${questions.size}",
                color = Color(0xFF6B7280),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(8.dp))

            // ── 본문 카드 ──
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xF2FFFFFF)),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "Day $day 퀴즈",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("다음 수어가 나타내는 단어를 선택하세요", color = Color(0xFF6B7280))

                    Spacer(Modifier.height(12.dp))

                    // 문제 영상(있으면 수동재생, 없으면 플레이스홀더)
                    if (current?.videoUrl != null) {
                        VideoPlayerManualPlay(url = current.videoUrl!!)
                    } else {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFE5F4EA)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("수어 동작", color = Color(0xFF16A34A))
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // 보기 리스트
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

                    // 제출 버튼
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

        // ── 정답/오답 다이얼로그 ──
        if (showResult && !finished && current != null) {
            AnswerDialog(
                correct = resultCorrect,
                correctWord = current.correct,
                onNext = {
                    showResult = false
                    selected = null
                    if (index + 1 < questions.size) index += 1 else finished = true
                }
            )
        }

        // ── 완료 다이얼로그 ──
        if (finished) {
            FinishDialog(
                day = day,
                score = score,
                total = questions.size,
                onRetry = {
                    // 다시 풀기
                    index = 0
                    selected = null
                    showResult = false
                    finished = false
                    score = 0
                },
                onGoRoadmap = onGoRoadmap
            )
        }
    }
}

/* ───────── 정답/오답 다이얼로그 ───────── */
@Composable
private fun AnswerDialog(
    correct: Boolean,
    correctWord: String,
    onNext: () -> Unit
) {
    val bg = if (correct) Color(0xFFE8FFF1) else Color(0xFFFFF1F2)
    val iconBg = if (correct) Color(0xFF16A34A) else Color(0xFFEF4444)
    val title = if (correct) "정답입니다!" else "오답입니다!"
    val sub   = if (correct) "잘했습니다" else "정답: $correctWord"

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

/* ───────── 완료 다이얼로그 ───────── */
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
                Text("$score / $total", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF16A34A))
                Spacer(Modifier.height(4.dp))
                Text("정답 개수를 확인해보세요", color = Color(0xFF6B7280))

                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("퀴즈 다시하기") }

                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onGoRoadmap,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("로드맵으로 돌아가기") }
            }
        },
        confirmButton = {},
        containerColor = Color.Transparent
    )
}

/* ───────── 세그먼트 토글(학습/퀴즈) ───────── */
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

        Text(left, color = leftColor,
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(leftBg)
                .clickable { onChange(true) }.padding(horizontal = 12.dp, vertical = 8.dp))
        Text(right, color = rightColor,
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(rightBg)
                .clickable { onChange(false) }.padding(horizontal = 12.dp, vertical = 8.dp))
    }
}

/* ───────── 영상(클릭 시에만 재생) ───────── */
@Composable
private fun VideoPlayerManualPlay(url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = false
        }
    }
    var isPlaying by remember { mutableStateOf(false) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener); player.release() }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true } },
            modifier = Modifier.matchParentSize()
        )
        if (!isPlaying) {
            Box(
                Modifier.matchParentSize().clickable { player.play() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.size(64.dp).clip(CircleShape).background(Color.White),
                    contentAlignment = Alignment.Center
                ) { Text("▶", color = Color(0xFF22C55E), fontSize = 24.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}


