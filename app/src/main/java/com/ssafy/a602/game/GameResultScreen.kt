package com.ssafy.a602.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.max

// 색상 팔레트
private val BackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF3D2C8D), // 보라
        Color(0xFF1F2A6B), // 남색
        Color(0xFF0E2149)  // 진한 남색
    )
)

private val CardBackground = Color(0xFF1B2454)
private val GradeGreen = Color(0xFF2ECC71)
private val AccuracyBlue = Color(0xFF7BB8FF)
private val ComboYellow = Color(0xFFFFD166)
private val PerfectBar = Color(0xFF6BB7FF)
private val MissBar = Color(0xFFFF6B6B)
private val MissItemBackground = Color(0xFF2E2A6B)
private val NewRecordBadge = Color(0xFFFFA72B)
private val NewRecordText = Color(0xFF0E2149)

/**
 * 게임 결과 화면
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun GameResultScreen(
    result: GameResultUi,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSubmitRanking: () -> Unit,
    onBackToList: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "게임 결과", 
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = result.songTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGradient)
        ) {
            val scroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 통합 요약 패널
                SummaryPanel(result = result)

                // 판정 분포
                JudgementDistributionCard(
                    perfect = result.correctCount,
                    miss = result.missCount
                )

                // 미스 단어 목록(보기 전용)
                MissWordListCard(words = result.missWords)
                
                // 액션 버튼들
                ResultActions(
                    onRetry = onRetry,
                    onSubmitRanking = onSubmitRanking,
                    onBackToList = onBackToList
                )
                
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// --- 유틸리티 함수들 ---

/**
 * 퍼센트 문자열 생성
 */
private fun pctStr(numer: Int, denom: Int): String {
    val d = max(denom, 1)
    val pct = (numer * 100f / d)
    return "${"%.1f".format(pct)}%"
}


// --- 컴포넌트: 통합 요약 패널 ---
@Composable
private fun SummaryPanel(result: GameResultUi) {
    // 등급별 포인트 색 (S/A/B/C/F)
    val gradeColor = when (result.grade) {
        "S" -> Color(0xFFFFD700) // Yellow
        "A" -> Color(0xFFFF3B30) // Red
        "B" -> Color(0xFF9C27B0) // Purple
        "C" -> Color(0xFF2196F3) // Blue
        "F" -> Color(0xFF424242) // Dark Gray
        else -> Color(0xFF9E9E9E)
    }
    // 내부 원(배경) 색
    val innerCircle = Color(0xFF17344A)

    // 천단위 콤마
    val scoreText = remember(result.score) {
        java.text.NumberFormat.getNumberInstance(java.util.Locale.getDefault())
            .format(result.score)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            if (result.isNewRecord) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(NewRecordBadge, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("신규 최고기록!", style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold, color = NewRecordText)
                }
            }

            // 중앙 정렬
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 링 스타일 등급 뱃지
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(width = 5.dp, color = gradeColor, shape = CircleShape) // 링
                        .background(innerCircle), // 내부 딥블루
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = result.grade,
                        // 글자는 등급 색으로
                        color = gradeColor,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // 총점
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = scoreText, // 콤마 적용
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "총점",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                // ====== 정확도 / 최대 콤보 (값 + 라벨) ======
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 정확도
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${result.accuracyPercent}%",
                            style = MaterialTheme.typography.titleLarge,
                            color = AccuracyBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "정확도",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    // 최대 콤보
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${result.maxCombo}",
                            style = MaterialTheme.typography.titleLarge,
                            color = ComboYellow,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "최대 콤보",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}



// --- 컴포넌트: 판정 분포 ---
@Composable
private fun JudgementDistributionCard(perfect: Int, miss: Int) {
    val total = (perfect + miss).coerceAtLeast(1)
    val perfectRatio = perfect / total.toFloat()
    val missRatio = miss / total.toFloat()

    val perfectPct = String.format("%.1f%%", perfectRatio * 100)
    val missPct = String.format("%.1f%%", missRatio * 100)

    // 팔레트(프로젝트 색상에 맞게 조정 가능)
    val perfectLabel = Color(0xFF7EB8FF)
    val missLabel    = Color(0xFFFF7B7B)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.8f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "판정 분포",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            JudgmentRow(
                label = "PERFECT",
                count = perfect,
                percentageText = perfectPct,
                ratio = perfectRatio,
                barColor = PerfectBar,
                labelColor = perfectLabel
            )

            JudgmentRow(
                label = "MISS",
                count = miss,
                percentageText = missPct,
                ratio = missRatio,
                barColor = MissBar,
                labelColor = missLabel
            )
        }
    }
}


@Composable
private fun JudgmentRow(
    label: String,            // "PERFECT" / "MISS"
    count: Int,               // 65 / 17
    percentageText: String,   // "79.3%" / "20.7%"
    ratio: Float,             // 0f..1f
    barColor: Color,          // PerfectBar / MissBar
    labelColor: Color,        // 레이블 색
    trackColor: Color = Color.White.copy(alpha = 0.10f)
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 좌측 레이블
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = labelColor,
            modifier = Modifier.width(82.dp) // 고정폭으로 정렬감
        )

        Spacer(Modifier.width(12.dp))

        // 진행 바
        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(trackColor)
        ) {
            // 채워진 구간
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(ratio.coerceIn(0f, 1f))
                    .background(barColor)
            )
            
            // 개수 표시 (채워진 구간의 끝에)
            if (count > 0) {
                BoxWithConstraints(Modifier.matchParentSize()) {
                    val filledWidth = constraints.maxWidth * ratio.coerceIn(0f, 1f)
                    val filledWidthDp = with(LocalDensity.current) { filledWidth.toDp() }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(filledWidthDp)
                    ) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(horizontal = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        // 우측 퍼센트
        Text(
            text = percentageText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}


// --- 컴포넌트: 미스 단어 목록(보기 전용) ---
@Composable
private fun MissWordListCard(words: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.8f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp), 
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = "경고",
                    tint = Color(0xFFFF7B7B),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "MISS", 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            if (words.isEmpty()) {
                Text(
                    text = "놓친 단어가 없습니다.", 
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                // 사각형 카드 형태의 항목들
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    words.forEach { word ->
                        MissWordItem(word = word)
                    }
                }
            }
        }
    }
}

@Composable
private fun MissWordItem(word: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4A2A2A).copy(alpha = 0.6f)) // 투명도가 있는 붉은빛 배경
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = word,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// --- 하단 액션 ---
@Composable
private fun ResultActions(
    onRetry: () -> Unit,
    onSubmitRanking: () -> Unit,
    onBackToList: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
            // 위 행: 다시하기, 랭킹에 전송
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 다시하기 버튼 (파란 배경)
                Button(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3772FF)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "다시하기",
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // 랭킹에 전송 버튼 (노란 배경)
                Button(
                    onClick = onSubmitRanking,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF5A524)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "명예의 전당",
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 아래 행: 목록으로 (회색/남색 무채 배경)
            Button(
                onClick = onBackToList,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2A2A6B)
                ),
                shape = RoundedCornerShape(12.dp)
            ) { 
                Text(
                    text = "목록으로", 
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center, 
                    modifier = Modifier.fillMaxWidth()
                ) 
            }
        }
}

// --- 프리뷰 ---
@Preview(showBackground = true)
@Composable
private fun GameResultScreenPreview() {
    val sample = GameResultUi(
        songTitle = "WAY BACK HOME",
        score = 876_420,
        accuracyPercent = 89,
        grade = "A",
        maxCombo = 27,
        correctCount = 65,
        missCount = 17,
        comboMultiplier = 1.2,
        isNewRecord = true,
        missWords = listOf("함께", "만들어", "기억", "별", "여름밤", "망령")
    )
    MaterialTheme {
        GameResultScreen(
            result = sample,
            onBack = {},
            onRetry = {},
            onSubmitRanking = {},
            onBackToList = {}
        )
    }
}
