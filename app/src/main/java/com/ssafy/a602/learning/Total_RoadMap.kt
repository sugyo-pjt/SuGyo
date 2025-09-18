@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ssafy.a602.learning

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
                    Box(Modifier.fillMaxSize()) {
                        // 중앙 라인
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(2.dp)
                                .align(Alignment.Center)
                                .background(Color(0xFFE0E0E0), RoundedCornerShape(1.dp))
                        )

                        LazyColumn(
                            contentPadding = PaddingValues(vertical = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(36.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(s.items, key = { _, it -> it.day }) { index, item ->
                                RoadmapRow(
                                    item = item,
                                    alignLeft = index % 2 == 0,
                                    onDayClick = onDayClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoadmapRow(
    item: DayItem,
    alignLeft: Boolean,
    onDayClick: (Int) -> Unit
) {
    val bubbleBrush = when (item.status) {
        DayStatus.DONE    -> Brush.linearGradient(listOf(Color(0xFF34D399), Color(0xFF10B981)))
        DayStatus.CURRENT -> Brush.linearGradient(listOf(Color(0xFF60A5FA), Color(0xFF3B82F6)))
        DayStatus.LOCKED  -> Brush.linearGradient(listOf(Color(0xFFE5E7EB), Color(0xFFD1D5DB)))
    }
    val textColor  = if (item.status == DayStatus.LOCKED) Color(0xFF9CA3AF) else Color.White
    val stickColor = if (item.status == DayStatus.LOCKED) Color(0xFFE5E7EB) else Color(0xFF9BE7C4)
    val enabled    = item.status != DayStatus.LOCKED

    Row(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (alignLeft) {
            DayBubble(item, bubbleBrush, textColor, Modifier.weight(0.5f), alignEnd = false, enabled, onDayClick)
            CenterStick(stickColor, Modifier.weight(0.5f), toLeft = false)
        } else {
            CenterStick(stickColor, Modifier.weight(0.5f), toLeft = true)
            DayBubble(item, bubbleBrush, textColor, Modifier.weight(0.5f), alignEnd = true, enabled, onDayClick)
        }
    }
}

@Composable
private fun DayBubble(
    item: DayItem,
    bubble: Brush,
    labelColor: Color,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false,
    enabled: Boolean,
    onDayClick: (Int) -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(bubble)
                .then(if (enabled) Modifier.clickable { onDayClick(item.day) } else Modifier)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Day ${item.day}",
                color = labelColor,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

@Composable
private fun CenterStick(color: Color, modifier: Modifier, toLeft: Boolean) {
    Row(modifier = modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
        if (toLeft) Spacer(Modifier.weight(0.5f))
        Box(
            modifier = Modifier
                .height(2.dp)
                .fillMaxWidth(0.4f)
                .background(color, RoundedCornerShape(1.dp))
        )
        if (!toLeft) Spacer(Modifier.weight(0.5f))
    }
}
