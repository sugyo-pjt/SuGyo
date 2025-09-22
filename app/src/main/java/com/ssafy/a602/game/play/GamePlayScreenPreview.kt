package com.ssafy.a602.game.play

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.ssafy.a602.game.ui.modern.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.a602.game.ui.*

/**
 * 프리뷰용 GamePlayScreen - 실제 데이터 없이도 UI 확인 가능
 */
@Composable
fun GamePlayScreenPreviewContent(
    songTitle: String = "샘플 곡",
    currentTime: Float = 45.2f,
    totalTime: Float = 180.0f,
    score: Int = 12345,
    combo: Int = 0, // 기본값을 0으로 변경하여 콤보 효과 확인 가능
    maxCombo: Int = 50,
    grade: String = "A",
    previousLyric: String = "이전 가사",
    currentLyric: String = "안녕하세요",
    nextLyric: String = "다음 가사",
    lyricProgress: Float = 0.6f,
    judgmentResult: JudgmentResult? = null, // 기본값은 null로 유지
    isPaused: Boolean = false
) {
    val bg = GameUITheme.Colors.DarkBackground

    Surface(modifier = Modifier.fillMaxSize(), color = bg) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { }
        ) {
            // 게임 배경
            GameBackground(isPlaying = !isPaused, combo = combo)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top bar - 실제 TopBarSection과 동일한 구조
                TopBarSection(
                    title = songTitle,
                    currentTime = currentTime,
                    totalDuration = totalTime,
                    isPaused = isPaused,
                    onTogglePause = { }
                )

                Spacer(Modifier.height(8.dp))

                // 전체 진행바 - 제목 바로 밑으로 이동 (실제 UI와 일치)
                GameProgressBar(
                    progress = if (totalTime > 0f) currentTime / totalTime else 0f,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                // 게임 상태 표시 (점수, 등급, 콤보) - 현재 주석 처리됨
                /*
                GameScoreCard(
                    score = score,
                    grade = grade,
                    maxCombo = maxCombo
                )
                */

                Spacer(Modifier.height(24.dp))

                // Camera area - 실제 카메라 프리뷰와 동일한 구조 (높이 300dp)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F2E)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        // 카메라 프리뷰 대신 샘플 영역
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF1A1F2E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "📹 카메라 영역",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "여기에 내 모습이 보입니다",
                                    color = Color(0xFF9AA3B2),
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "콤보: $combo",
                                    color = GameUITheme.Colors.NeonLime,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        // 판정 오버레이 (실제 UI와 동일)
                        judgmentResult?.let { result ->
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                // 판정 타입에 따른 동적 텍스트와 색상
                                val (text, color) = when (result.type) {
                                    JudgmentType.PERFECT -> Pair("PERFECT!", GameUITheme.Colors.Perfect)
                                    JudgmentType.GREAT -> Pair("GREAT!", GameUITheme.Colors.Great)
                                    JudgmentType.GOOD -> Pair("GOOD!", GameUITheme.Colors.Good)
                                    JudgmentType.MISS -> Pair("MISS!", GameUITheme.Colors.Miss)
                                    else -> Pair("PERFECT!", GameUITheme.Colors.Perfect)
                                }
                                
                                Text(
                                    text = text,
                                    color = color,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 가사 영역 - 실제 UI와 동일한 구조 (높이 250dp, 파도 효과 포함)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                ) {
                    // 파도 효과 배경 (콤보에 따른 색상 변화)
                    GameBackground(
                        modifier = Modifier.fillMaxSize(),
                        isPlaying = !isPaused,
                        combo = combo
                    )

                    // 가사 카드
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        colors = CardDefaults.cardColors(containerColor = Color(0x801A1F2E)), // 50% 투명도
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // 가사 그룹 (중앙)
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // 이전 가사
                                Text(
                                    text = previousLyric,
                                    color = Color(0xFF9AA3B2),
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                
                                Spacer(Modifier.height(6.dp))
                                
                                // 현재 가사 (메인) - 수어 하이라이팅 적용 (프리뷰용)
                                val highlightedText = buildAnnotatedString {
                                    // 프리뷰에서는 가사의 일부를 빨간색으로 표시
                                    val text = currentLyric
                                    val highlightStart = if (text.length > 3) 1 else 0
                                    val highlightEnd = if (text.length > 3) 3 else text.length
                                    
                                    if (highlightStart < text.length) {
                                        append(text.substring(0, highlightStart))
                                        withStyle(style = androidx.compose.ui.text.SpanStyle(color = Color.Red)) {
                                            append(text.substring(highlightStart, highlightEnd))
                                        }
                                        if (highlightEnd < text.length) {
                                            append(text.substring(highlightEnd))
                                        }
                                    } else {
                                        append(text)
                                    }
                                }
                                
                                Text(
                                    text = highlightedText,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                
                                Spacer(Modifier.height(6.dp))
                                
                                // 다음 가사
                                Text(
                                    text = nextLyric,
                                    color = Color(0xFF6B7280),
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            
                            // 진행률 표시 (하단)
                            LinearProgressIndicator(
                                progress = { lyricProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = Color(0xFF4CAF50),
                                trackColor = Color(0xFF2A2F3E)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Bottom button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { },
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

            // 게임 오버레이 효과들
            GameComboAura(combo = combo, modifier = Modifier.align(Alignment.Center))
            
            // 판정 토스트 (중앙에 표시)
            if (judgmentResult != null) {
                GameJudgmentToast(
                    result = judgmentResult, 
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    backgroundColor = 0xFF0B0E13
)
@Composable
fun GamePlayScreenPreviewDefault() {
    GamePlayScreenPreviewContent(
        songTitle = "기본 상태",
        combo = 0, // 콤보 없음
        judgmentResult = null // 판정 없음
    )
}

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    backgroundColor = 0xFF0B0E13
)
@Composable
fun GamePlayScreenPreviewWithPerfect() {
    GamePlayScreenPreviewContent(
        songTitle = "완벽한 게임",
        currentTime = 120.5f,
        totalTime = 240.0f,
        score = 98765,
        combo = 75,
        maxCombo = 100,
        grade = "S",
        previousLyric = "이전 구간",
        currentLyric = "완벽한 수어",
        nextLyric = "다음 구간",
        lyricProgress = 0.8f,
        judgmentResult = JudgmentResult(
            type = JudgmentType.PERFECT,
            accuracy = 0.98f,
            score = 1000,
            combo = 75,
            timestamp = System.currentTimeMillis()
        )
    )
}

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    backgroundColor = 0xFF0B0E13
)
@Composable
fun GamePlayScreenPreviewWithMiss() {
    GamePlayScreenPreviewContent(
        songTitle = "연습 중",
        currentTime = 30.0f,
        totalTime = 180.0f,
        score = 5432,
        combo = 0,
        maxCombo = 15,
        grade = "C",
        previousLyric = "이전 가사",
        currentLyric = "연습이 필요해요",
        nextLyric = "다음 가사",
        lyricProgress = 0.2f,
        judgmentResult = JudgmentResult(
            type = JudgmentType.MISS,
            accuracy = 0.0f,
            score = 0,
            combo = 0,
            timestamp = System.currentTimeMillis()
        )
    )
}

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    backgroundColor = 0xFF0B0E13
)
@Composable
fun GamePlayScreenPreviewHighCombo() {
    GamePlayScreenPreviewContent(
        songTitle = "콤보 마스터",
        currentTime = 90.0f,
        totalTime = 200.0f,
        score = 45678,
        combo = 150,
        maxCombo = 200,
        grade = "SS",
        previousLyric = "이전 가사",
        currentLyric = "콤보가 계속 올라가요",
        nextLyric = "다음 가사",
        lyricProgress = 0.5f,
        judgmentResult = JudgmentResult(
            type = JudgmentType.PERFECT,
            accuracy = 0.95f,
            score = 1500,
            combo = 150,
            timestamp = System.currentTimeMillis()
        )
    )
}

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    backgroundColor = 0xFF0B0E13
)
@Composable
fun GamePlayScreenPreviewComboAura() {
    GamePlayScreenPreviewContent(
        songTitle = "콤보 오라 테스트",
        currentTime = 60.0f,
        totalTime = 180.0f,
        score = 25000,
        combo = 75,
        maxCombo = 100,
        grade = "A",
        previousLyric = "이전 가사",
        currentLyric = "콤보 오라가 보이나요?",
        nextLyric = "다음 가사",
        lyricProgress = 0.4f,
        judgmentResult = null // 콤보 오라만 보이도록
    )
}

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    backgroundColor = 0xFF0B0E13
)
@Composable
fun GamePlayScreenPreviewJudgmentToast() {
    GamePlayScreenPreviewContent(
        songTitle = "판정 토스트 테스트",
        currentTime = 45.0f,
        totalTime = 150.0f,
        score = 15000,
        combo = 30,
        maxCombo = 50,
        grade = "B",
        previousLyric = "이전 가사",
        currentLyric = "판정 토스트가 보이나요?",
        nextLyric = "다음 가사",
        lyricProgress = 0.3f,
        judgmentResult = JudgmentResult(
            type = JudgmentType.GREAT,
            accuracy = 0.85f,
            score = 800,
            combo = 30,
            timestamp = System.currentTimeMillis()
        )
    )
}

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    backgroundColor = 0xFF0B0E13
)
@Composable
fun GamePlayScreenPreviewWaveEffect() {
    GamePlayScreenPreviewContent(
        songTitle = "파도 효과 테스트",
        currentTime = 30.0f,
        totalTime = 120.0f,
        score = 8000,
        combo = 15,
        maxCombo = 25,
        grade = "C",
        previousLyric = "이전 가사",
        currentLyric = "파도 효과가 보이나요?",
        nextLyric = "다음 가사",
        lyricProgress = 0.25f,
        judgmentResult = null, // 파도 효과만 보이도록
        isPaused = false // 재생 중으로 설정하여 파도 효과 활성화
    )
}

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    backgroundColor = 0xFF0B0E13
)
@Composable
fun GamePlayScreenPreviewAllEffects() {
    GamePlayScreenPreviewContent(
        songTitle = "모든 효과 테스트",
        currentTime = 75.0f,
        totalTime = 200.0f,
        score = 35000,
        combo = 100,
        maxCombo = 150,
        grade = "S",
        previousLyric = "이전 가사",
        currentLyric = "모든 효과가 보이나요?",
        nextLyric = "다음 가사",
        lyricProgress = 0.6f,
        judgmentResult = JudgmentResult(
            type = JudgmentType.PERFECT,
            accuracy = 0.98f,
            score = 1200,
            combo = 100,
            timestamp = System.currentTimeMillis()
        ),
        isPaused = false
    )
}

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    backgroundColor = 0xFF0B0E13
)
@Composable
fun GamePlayScreenPreviewPerfect() {
    GamePlayScreenPreviewContent(
        songTitle = "Perfect 판정 테스트",
        currentTime = 60.0f,
        totalTime = 180.0f,
        score = 25000,
        combo = 20, // 낮은 콤보로 기본 배경 색상 확인
        maxCombo = 75,
        grade = "S",
        previousLyric = "이전 가사",
        currentLyric = "Perfect 판정",
        nextLyric = "다음 가사",
        lyricProgress = 0.4f,
        judgmentResult = JudgmentResult(
            type = JudgmentType.PERFECT,
            accuracy = 0.95f,
            score = 1000,
            combo = 20,
            timestamp = System.currentTimeMillis()
        )
    )
}

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    backgroundColor = 0xFF0B0E13
)
@Composable
fun GamePlayScreenPreviewGreat() {
    GamePlayScreenPreviewContent(
        songTitle = "Great 판정 테스트",
        currentTime = 45.0f,
        totalTime = 150.0f,
        score = 18000,
        combo = 50, // 중간 콤보로 색상 변화 확인
        maxCombo = 50,
        grade = "A",
        previousLyric = "이전 가사",
        currentLyric = "Great 판정",
        nextLyric = "다음 가사",
        lyricProgress = 0.3f,
        judgmentResult = JudgmentResult(
            type = JudgmentType.GREAT,
            accuracy = 0.85f,
            score = 800,
            combo = 50,
            timestamp = System.currentTimeMillis()
        )
    )
}

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    backgroundColor = 0xFF0B0E13
)
@Composable
fun GamePlayScreenPreviewGood() {
    GamePlayScreenPreviewContent(
        songTitle = "Good 판정 테스트",
        currentTime = 30.0f,
        totalTime = 120.0f,
        score = 12000,
        combo = 100, // 높은 콤보로 색상 변화 확인
        maxCombo = 25,
        grade = "B",
        previousLyric = "이전 가사",
        currentLyric = "Good 판정",
        nextLyric = "다음 가사",
        lyricProgress = 0.25f,
        judgmentResult = JudgmentResult(
            type = JudgmentType.GOOD,
            accuracy = 0.70f,
            score = 500,
            combo = 100,
            timestamp = System.currentTimeMillis()
        )
    )
}

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    backgroundColor = 0xFF0B0E13
)
@Composable
fun GamePlayScreenPreviewMiss() {
    GamePlayScreenPreviewContent(
        songTitle = "Miss 판정 테스트",
        currentTime = 20.0f,
        totalTime = 100.0f,
        score = 5000,
        combo = 0, // 콤보 리셋으로 기본 배경 색상 확인
        maxCombo = 10,
        grade = "C",
        previousLyric = "이전 가사",
        currentLyric = "Miss 판정",
        nextLyric = "다음 가사",
        lyricProgress = 0.2f,
        judgmentResult = JudgmentResult(
            type = JudgmentType.MISS,
            accuracy = 0.0f,
            score = 0,
            combo = 0,
            timestamp = System.currentTimeMillis()
        )
    )
}
