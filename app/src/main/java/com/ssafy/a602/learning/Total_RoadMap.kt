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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("로드맵", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                        Text("←", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .background(Color(0xFFF1FBF4))
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

/* ──────────────────────────────────────────────────────────────────────────
 * 귀여운 지그재그 로드맵 + babyshark 데코
 * ────────────────────────────────────────────────────────────────────────── */
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

        LazyColumn(
            contentPadding = PaddingValues(top = 32.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(items, key = { _, it -> it.day }) { index, item ->
                val alignLeft = index % 2 == 0

                Column {
                    // Day 노드 한 줄
                    ZigzagRow(
                        item = item,
                        alignLeft = alignLeft,
                        onDayClick = onDayClick
                    )

                    // Day들 사이에 babyshark 데코 넣기 (규칙은 자유롭게 바꿔도 OK)
                    if (index < items.lastIndex && index % 2 == 0) {
                        Spacer(Modifier.height(6.dp))
                        MascotBetweenImage(
                            resId = AppR.drawable.babyshark,
                            onLeftSide = (index % 4 == 0),
                            size = 200.dp,             // ← 필요시 48.dp로 더 키워도 OK
                            offsetFromCenter = 24.dp  // ← 아이콘이 커졌으니 살짝 더 떨어뜨림
                        )
                    }
                }
            }
        }

        // 하단 말풍선(옵션)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFDCFCE7))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text("🐥 파이팅!", fontWeight = FontWeight.SemiBold, color = Color(0xFF16A34A))
        }
    }
}

/* ──────────────────────────────────────────────────────────────────────────
 * 지그재그 한 줄 (weight는 여기서만 적용)
 * ────────────────────────────────────────────────────────────────────────── */
@Composable
private fun ZigzagRow(
    item: DayItem,
    alignLeft: Boolean,
    onDayClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
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

/* ──────────────────────────────────────────────────────────────────────────
 * Day 노드(상태별 스킨 + CURRENT 바운스)
 * ────────────────────────────────────────────────────────────────────────── */
@Composable
private fun CuteDayNode(
    item: DayItem,
    alignEnd: Boolean,                 // false: 왼쪽 열(Start), true: 오른쪽 열(End)
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

    Box(
        modifier = modifier
            .padding(horizontal = 8.dp)   // 기본 여백
            .height(96.dp),
        contentAlignment = if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart
    ) {
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

/* ──────────────────────────────────────────────────────────────────────────
 * 커넥터(점선) — 중앙 라인 근처에서 살짝만
 * ────────────────────────────────────────────────────────────────────────── */
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
        modifier = modifier
            .height(96.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (toLeft) Spacer(Modifier.weight(0.5f))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(0.40f) // 살짝 짧게
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

/* ──────────────────────────────────────────────────────────────────────────
 * 지그재그 사이에 끼워 넣는 babyshark 데코(이미지)
 *  - onLeftSide=true면 중앙 라인 왼쪽에, false면 오른쪽에
 *  - 살짝 바운스/페이드로 생동감
 * ────────────────────────────────────────────────────────────────────────── */
@Composable
private fun MascotBetweenImage(
    resId: Int,
    onLeftSide: Boolean,
    size: Dp = 44.dp,          // ← 크기 키움 (원하면 48dp, 56dp 등으로)
    offsetFromCenter: Dp = 24.dp // ← 중앙 라인으로부터 거리
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
            .height(size + 8.dp), // 데코 높이는 아이콘 크기에 맞춰 조정
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
                contentDescription = null, // 데코용
                modifier = Modifier
                    .offset(x = if (onLeftSide) -offsetFromCenter else offsetFromCenter)
                    .graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha)
                    .size(size)
            )
        }

        if (onLeftSide) Spacer(Modifier.weight(0.5f))
    }
}

