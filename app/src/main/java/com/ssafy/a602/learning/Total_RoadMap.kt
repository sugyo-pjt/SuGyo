@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ssafy.a602.learning

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.a602.R as AppR

@Composable
fun Total_RoadMap(
    onBack: () -> Unit = {},
    onDayClick: (Int) -> Unit = {},
    viewModel: RoadmapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 상단바
        CenterAlignedTopAppBar(
            modifier = Modifier
                .statusBarsPadding()
                .background(Color(0xFFF1FBF4)),
            windowInsets = WindowInsets(0),
            title = { Text("로드맵", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로가기")
                }
            }
        )
        
        // 로드맵 내용 (초록 배경)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF1FBF4))
                .padding(bottom = 24.dp) // 하단 네비게이션 여백
        ) {
            when (val s = uiState) {
                is RoadmapUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is RoadmapUiState.Error -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(s.message, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.reload() }) { Text("다시 시도") }
                    }
                }
                is RoadmapUiState.Success -> {
                    CuteRoadmapList(items = s.items, onDayClick = onDayClick)
                }
            }
        }
    }
}

/* ──────────────────────────────────────────────────────────────────────── */
@Composable
private fun CuteRoadmapList(
    items: List<DayItem>,
    onDayClick: (Int) -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        // 중앙 가이드 라인
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(2.dp)
                .align(Alignment.Center)
                .background(Color(0xFFE9F4EC))
        )

        val navBarsPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        LazyColumn(
            // ✅ 하단 잘림 방지: 내비게이션 inset + 여유 80.dp
            contentPadding = PaddingValues(top = 20.dp, bottom = navBarsPadding + 80.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(items, key = { _, it -> it.day }) { index, item ->
                val alignLeft = index % 2 == 0

                Column {
                    ZigzagRow(
                        item = item,
                        alignLeft = alignLeft,
                        onDayClick = onDayClick
                    )

                    // 마스코트 데코(선택)
                    if (index < items.lastIndex && index % 2 == 0) {
                        Spacer(Modifier.height(6.dp))
                        MascotBetweenImage(
                            resId = AppR.drawable.babyshark,
                            onLeftSide = (index % 4 == 0),
                            size = 180.dp,
                            offsetFromCenter = 20.dp
                        )
                    }
                }
            }

            // ✅ 리스트 끝 여유 공간(혹시 모를 절단 방지)
            item { Spacer(Modifier.height(12.dp)) }
        }

        // 좌하단 응원 말풍선
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = navBarsPadding + 12.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFDCFCE7))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text("🐥 파이팅!", fontWeight = FontWeight.SemiBold, color = Color(0xFF16A34A))
        }
    }
}

/* ──────────────────────────────────────────────────────────────────────── */
@Composable
private fun ZigzagRow(
    item: DayItem,
    alignLeft: Boolean,
    onDayClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp), // ✅ 높이 상향(별/칩 포함 안전)
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (alignLeft) {
            CuteDayNode(
                item = item,
                alignEnd = false,
                onDayClick = onDayClick,
                modifier = Modifier.weight(0.5f)
            )
            CuteConnector(
                toLeft = false,
                status = item.status,
                modifier = Modifier.weight(0.5f)
            )
        } else {
            CuteConnector(
                toLeft = true,
                status = item.status,
                modifier = Modifier.weight(0.5f)
            )
            CuteDayNode(
                item = item,
                alignEnd = true,
                onDayClick = onDayClick,
                modifier = Modifier.weight(0.5f)
            )
        }
    }
}

/* ──────────────────────────────────────────────────────────────────────── */
@Composable
private fun CuteDayNode(
    item: DayItem,
    alignEnd: Boolean, // false: 왼쪽 열, true: 오른쪽 열
    onDayClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val enabled = item.status != DayStatus.LOCKED

    val (brush, labelColor, emoji) = when (item.status) {
        DayStatus.DONE -> Triple(
            Brush.linearGradient(listOf(Color(0xFF34D399), Color(0xFF10B981))),
            Color.White, "⭐"
        )
        DayStatus.CURRENT -> Triple(
            Brush.linearGradient(listOf(Color(0xFF60A5FA), Color(0xFF3B82F6))),
            Color.White, "▶"
        )
        DayStatus.LOCKED -> Triple(
            Brush.linearGradient(listOf(Color(0xFFE5E7EB), Color(0xFFD1D5DB))),
            Color(0xFF9CA3AF), "🔒"
        )
    }

    // CURRENT만 바운스
    val scale: Float = if (item.status == DayStatus.CURRENT) {
        val t: InfiniteTransition = rememberInfiniteTransition(label = "bounce")
        t.animateFloat(
            initialValue = 1f, targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bounceValue"
        ).value
    } else 1f

    // 정렬 기준(좌/우 열)
    val boxAlign = if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart
    val columnAlign = if (alignEnd) Alignment.End else Alignment.Start

    Box(
        modifier = modifier.padding(horizontal = 8.dp),
        contentAlignment = boxAlign
    ) {
        Column(horizontalAlignment = columnAlign) {
            // 원형 Day 버튼
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .clip(CircleShape)
                    .background(brush)
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        clip = false,
                        ambientColor = Color(0x3322C55E),
                        spotColor = Color(0x3322C55E)
                    )
                    .then(if (enabled) Modifier.clickable { onDayClick(item.day) } else Modifier)
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(emoji, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Day ${item.day}",
                        color = labelColor,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ✅ 별/점수는 원 "아래"에 정석 배치 (오프셋 X)
            if (item.status == DayStatus.DONE && item.correctCount != null && item.totalCount != null) {
                Spacer(Modifier.height(6.dp))
                QuizResultStars(
                    correctCount = item.correctCount,
                    totalCount = item.totalCount
                )
            }
        }

        // START 리본 (현재 진행중)
        if (item.status == DayStatus.CURRENT) {
            Box(
                modifier = Modifier
                    .align(if (alignEnd) Alignment.TopEnd else Alignment.TopStart)
                    .offset(x = if (alignEnd) (-6).dp else 6.dp, y = (-8).dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFDCFCE7))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("START", color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
            }
        }
    }
}

/* ──────────────────────────────────────────────────────────────────────── */

/* ──────────────────────────────────────────────────────────────────────── */
@Composable
private fun CuteConnector(
    toLeft: Boolean,
    status: DayStatus,
    modifier: Modifier = Modifier
) {
    val color = when (status) {
        DayStatus.LOCKED  -> Color(0xFFE5E7EB)
        DayStatus.DONE    -> Color(0xFF9BE7C4)
        DayStatus.CURRENT -> Color(0xFFBFE3FF)
    }

    Row(
        modifier = modifier.height(112.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (toLeft) Spacer(Modifier.weight(0.5f))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(0.40f)
        ) {
            repeat(5) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(Modifier.width(6.dp))
            }
        }

        if (!toLeft) Spacer(Modifier.weight(0.5f))
    }
}

/* ──────────────────────────────────────────────────────────────────────── */
@Composable
private fun MascotBetweenImage(
    resId: Int,
    onLeftSide: Boolean,
    size: Dp = 44.dp,
    offsetFromCenter: Dp = 24.dp
) {
    val t = rememberInfiniteTransition(label = "shark-bob")
    val scale by t.animateFloat(
        0.98f, 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by t.animateFloat(
        0.9f, 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(size + 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!onLeftSide) Spacer(Modifier.weight(0.5f))

        Box(
            modifier = Modifier
                .weight(0.5f)
                .padding(horizontal = 8.dp)
                .height(size + 8.dp),
            contentAlignment = if (onLeftSide) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Image(
                painter = painterResource(id = resId),
                contentDescription = null,
                modifier = Modifier
                    .offset(x = if (onLeftSide) -offsetFromCenter else offsetFromCenter)
                    .graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha)
                    .size(size)
            )
        }

        if (onLeftSide) Spacer(Modifier.weight(0.5f))
    }
}

/* ──────────────────────────────────────────────────────────────────────── */
@Composable
private fun QuizResultStars(
    correctCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    val percentage = if (totalCount > 0) correctCount.toFloat() / totalCount.toFloat() else 0f
    val filledStars = (percentage * 5).toInt()
    val hasHalfStar = (percentage * 5) % 1 >= 0.5f // (현재는 반쪽 별 아이콘이 없어 미사용)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 별 5개
        repeat(5) { index ->
            val isFilled = index < filledStars
            Icon(
                imageVector = if (isFilled) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = null,
                tint = if (isFilled) Color(0xFFFFD700) else Color(0xFFE5E7EB),
                modifier = Modifier.size(12.dp)
            )
        }
        
        // 점수 표시 (배경색 없이)
        Text(
            text = "$correctCount/$totalCount",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF6B7280)
        )
    }
}
