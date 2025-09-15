
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
import androidx.compose.runtime.rememberCoroutineScope   // ⬅️ 추가
import kotlinx.coroutines.launch                         // ⬅️ 추가

/* ───────────────────────── 내부 전용 모델 ───────────────────────── */
// 캐시 아이템 → 퀴즈 문제로 변환해 쓸 로컬 모델
private data class Question(
    val videoUrl: String?,      // 문제 영상(없으면 텍스트 플레이스홀더)
    val correct: String,        // 정답 단어(텍스트)
    val options: List<String>   // 보기 텍스트(셔플 완료)
)

/* ─────────── 캐시 목록 → Question 리스트 생성 ─────────── */
private fun buildQuestionsFromCache(
    cache: List<LearningMemCache.Item>,
    count: Int = 10,            // 만들 문제 수(캐시 크기보다 크면 캐시 크기로 제한)
    optionsPerQuestion: Int = 4 // 보기 수(기본 4지선다)
): List<Question> {
    if (cache.isEmpty()) return emptyList()

    val base = cache.shuffled()
    val qCount = minOf(count, base.size)

    return (0 until qCount).map { idx ->
        val correct = base[idx]
        val otherWords = cache
            .filter { it.word != correct.word }
            .shuffled()
            .take(optionsPerQuestion - 1)
            .map { it.word }

        val options = (otherWords + correct.word).shuffled()
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

    // ⬇️ 추가: 이벤트(버튼/다이얼로그)에서 비동기 호출할 때 쓰는 UI 스코프
    val scope = rememberCoroutineScope()

    // ✅ 학습 화면에서 저장해둔 캐시를 가져온다
    val cached = remember(day) { LearningMemCache.get(day) }

    // 캐시가 없으면 안내(학습 화면에서 다시 시도 유도)
    if (cached.isNullOrEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().background(bg).padding(24.dp),
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

    // ── 퀴즈 진행 상태 ───────────────────────────────────────────────
    val questions = remember(cached) { buildQuestionsFromCache(cached, count = 10, optionsPerQuestion = 4) }
    var index by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf<String?>(null) }
    var showResult by remember { mutableStateOf(false) }
    var resultCorrect by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }
    var finished by remember { mutableStateOf(false) }

    // 점수 저장 진행 상태(MVP; 실제 Retrofit 자리)
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
            // ── 상단 바 ─────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("←",
                    fontSize = 20.sp,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onBack() }.padding(4.dp))
                Spacer(Modifier.width(8.dp))
                Text("Day $day", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.weight(1f))
                SegmentedTwoToggle(
                    left = "학습 모드",
                    right = "퀴즈 모드",
                    selectedLeft = false,
                    onChange = { left -> if (left) onGoStudy(day) }
                )
            }

            Spacer(Modifier.height(8.dp))
            Text("${minOf(index + 1, questions.size)}/${questions.size}",
                color = Color(0xFF6B7280),
                modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(8.dp))

            // ── 본문 카드 ──────────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xF2FFFFFF)),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Day $day 퀴즈", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(Modifier.height(6.dp))
                    Text("다음 수어가 나타내는 단어를 선택하세요", color = Color(0xFF6B7280))
                    Spacer(Modifier.height(12.dp))

                    // 문제(영상/플레이스홀더)
                    if (current?.videoUrl != null) {
                        VideoPlayerManualPlay(url = current.videoUrl)
                    } else {
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(16f / 9f)
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
                                    .clickable { selected = opt }  // 선택만, 채점은 제출 때
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
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) { Text("제출하기") }
                }
            }
        }

        // ── 정답/오답 다이얼로그 ────────────────────────────────────
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
                        // 마지막 문제 → 완료 플래그 올리고 점수 저장
                        finished = true
                        // ▼ 서버 저장 (dayId + score) — 실패해도 UI는 완료로 유지
                        submitInProgress = true
                        submitOk = null
                        // 실제론 ViewModel/CoroutineScope로 보내도 OK(여긴 간단히)
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

        // ── 완료 다이얼로그 ─────────────────────────────────────────
        if (finished) {
            FinishDialog(
                day = day,
                score = score,
                total = questions.size,
                onRetry = { // 다시 풀기
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

            // 저장 상태 보조 표시
            if (submitInProgress) {
                Text("결과 저장 중…", color = Color(0xFF6B7280),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp))
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

/* ───────── 정답/오답/완료 다이얼로그 & 토글/플레이어 (기존과 동일) ───────── */
// (아래 컴포넌트들은 네 기존 코드와 동일하므로 그대로 둬도 됩니다)

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
                    Text(
                        if (correct) "✓" else "✕",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
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


/* ───────── 서버 전송 (MVP: 더미) ───────── */
// 실제 Retrofit/ktor로 교체하면 됨.
// Body 예시: { "dayId": day, "score": score }
// 서버에서 total도 원하면 함께 보내기: { "dayId": day, "score": score, "total": total }
private suspend fun submitClientScore(
    day: Int,
    score: Int,
    total: Int
): Boolean {
    // TODO: Retrofit으로 POST /learning/days/{day}/submissions 교체
    delay(400) // 네트워크 지연 흉내
    return true // 성공했다고 가정
}
