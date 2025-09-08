@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
// ↑ material3 상단바 API가 현재 릴리스에서 Experimental로 표시되어 있어
//   파일 전체에서 “사용에 동의(Opt-in)”한다는 뜻.
//   함수마다 @OptIn(...)을 붙이는 대신 파일 한 줄로 끝내는 방식이 깔끔함.

package com.ssafy.a602.learning

// ─────────────────────────────────────────────────────────────────────────────
// [필요 임포트]
//  - CenterAlignedTopAppBar : material3 상단바. 일부 릴리스에서 Experimental이라 위에 Opt-in.
//  - remember/LaunchedEffect : Compose 상태/사이드이펙트.
//  - rememberCoroutineScope : onClick(Composable 아님)에서 코루틴을 돌릴 때 사용.
// ─────────────────────────────────────────────────────────────────────────────
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// [모델/상태] — UI에서 쓰기 쉬운 형태로 타입을 분리
// ─────────────────────────────────────────────────────────────────────────────
private enum class DayStatus { DONE, CURRENT, LOCKED }  // 완료/진행중/잠금

private data class RoadmapMeta(
    val totalDays: Int,   // 서버가 내려주는 “총 일수”(예: 5 → Day1..Day5 생성)
    val progressDay: Int  // 서버가 내려주는 “진행상황”(예: 3 → 1~3 완료, 4는 현재, 5는 잠금)
)

private data class DayItem(
    val day: Int,
    val status: DayStatus
)

private sealed interface UiState {
    data object Loading : UiState                 // 로딩 스피너
    data class Success(val items: List<DayItem>) : UiState // 데이터 표시
    data class Error(val throwable: Throwable) : UiState   // 에러 표시/재시도
}

// ─────────────────────────────────────────────────────────────────────────────
// [가짜 백엔드] — 실제 API로 갈아끼울 자리
//  - Retrofit/ktor 호출로 변경하면 됨. delay는 네트워크 지연 흉내.
// ─────────────────────────────────────────────────────────────────────────────
private suspend fun fetchRoadmapMetaFromServer(): RoadmapMeta {
    delay(200)
    return RoadmapMeta(totalDays = 5, progressDay = 3)
}

// ─────────────────────────────────────────────────────────────────────────────
// [루트 컴포저블] — 전체 화면
//  - Scaffold로 상단바/본문 레이아웃을 구성
//  - LaunchedEffect로 초기에 한 번 로딩
//  - onClick(컴포저블 아님)에서는 rememberCoroutineScope().launch { ... } 사용
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun Total_RoadMap(
    onBack: () -> Unit = {},                  // 상단바 뒤로가기 콜백(보통 navController.popBackStack)
    onDayClick: (Int) -> Unit = {}            // Day 버튼 눌렀을 때 콜백(예: navController.navigate("lesson/$day"))
) {
    var uiState by remember { mutableStateOf<UiState>(UiState.Loading) }
    val scope = rememberCoroutineScope()

    // 화면 진입 시 1회 로드
    LaunchedEffect(Unit) {
        reload(setState = { uiState = it })
    }

    Scaffold(
        topBar = {
            // material3 상단바. Experimental이라 파일 맨 위에서 Opt-in 처리
            CenterAlignedTopAppBar(
                title = { Text("로드맵", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    // 간단한 텍스트← 버튼. 실제 앱에선 IconButton으로 교체해도 됨.
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
                .padding(inner)                // Scaffold의 insets
                .background(Color(0xFFF1FBF4)) // 연한 그린 배경
        ) {
            when (val s = uiState) {
                UiState.Loading -> {
                    // 로딩 스피너
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Error -> {
                    // 에러 메시지 + 재시도 버튼
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("로드맵을 불러오지 못했어요.", color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                // onClick은 컴포저블이 아님 → 코루틴으로 재호출
                                scope.launch { reload(setState = { uiState = it }) }
                            }
                        ) { Text("다시 시도") }
                    }
                }

                is UiState.Success -> {
                    // 중앙 세로 라인 + 좌/우 번갈아 나오는 Day 아이템들
                    Box(Modifier.fillMaxSize()) {
                        // 중앙 라인 (얇은 세로선)
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
                                    alignLeft = index % 2 == 0,   // 0,2,4...는 왼쪽 / 1,3,5...는 오른쪽
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

// ─────────────────────────────────────────────────────────────────────────────
// [로딩 함수] — 서버에서 메타를 받아와 UI용 아이템으로 가공
//  - day ≤ progress -> DONE
//  - day == progress+1 -> CURRENT
//  - 그 외 -> LOCKED
// ─────────────────────────────────────────────────────────────────────────────
private suspend fun reload(setState: (UiState) -> Unit) {
    setState(UiState.Loading)
    try {
        val meta = fetchRoadmapMetaFromServer()
        val items = (1..meta.totalDays).map { day ->
            val status = when {
                day <= meta.progressDay     -> DayStatus.DONE
                day == meta.progressDay + 1 -> DayStatus.CURRENT
                else                        -> DayStatus.LOCKED
            }
            DayItem(day, status)
        }
        setState(UiState.Success(items))
    } catch (t: Throwable) {
        setState(UiState.Error(t))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// [한 줄(행) 레이아웃] — 중앙 라인을 기준으로 Day 말풍선 + 짧은 보조선
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RoadmapRow(
    item: DayItem,
    alignLeft: Boolean,
    onDayClick: (Int) -> Unit
) {
    // 상태별 그라데이션/색상
    val bubbleBrush = when (item.status) {
        DayStatus.DONE    -> Brush.linearGradient(listOf(Color(0xFF34D399), Color(0xFF10B981))) // 초록
        DayStatus.CURRENT -> Brush.linearGradient(listOf(Color(0xFF60A5FA), Color(0xFF3B82F6))) // 파랑
        DayStatus.LOCKED  -> Brush.linearGradient(listOf(Color(0xFFE5E7EB), Color(0xFFD1D5DB))) // 회색
    }
    val textColor  = if (item.status == DayStatus.LOCKED) Color(0xFF9CA3AF) else Color.White
    val stickColor = if (item.status == DayStatus.LOCKED) Color(0xFFE5E7EB) else Color(0xFF9BE7C4)
    val enabled    = item.status != DayStatus.LOCKED

    Row(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (alignLeft) {
            // 말풍선(왼쪽) + 중앙 보조선
            DayBubble(item, bubbleBrush, textColor, Modifier.weight(0.5f), alignEnd = false, enabled, onDayClick)
            CenterStick(stickColor, Modifier.weight(0.5f), toLeft = false)
        } else {
            // 중앙 보조선 + 말풍선(오른쪽)
            CenterStick(stickColor, Modifier.weight(0.5f), toLeft = true)
            DayBubble(item, bubbleBrush, textColor, Modifier.weight(0.5f), alignEnd = true, enabled, onDayClick)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// [원형 Day 말풍선] — 클릭 가능/불가를 상태로 제어
// ─────────────────────────────────────────────────────────────────────────────
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
                // enabled=false면 clickable 제거
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

// ─────────────────────────────────────────────────────────────────────────────
// [중앙 보조선] — 중앙 라인 기준으로 바깥으로 뻗는 짧은 가로선
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CenterStick(color: Color, modifier: Modifier, toLeft: Boolean) {
    Row(modifier = modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
        if (toLeft) Spacer(Modifier.weight(0.5f))    // 왼쪽에 빈 공간 절반
        Box(
            modifier = Modifier
                .height(2.dp)
                .fillMaxWidth(0.4f)                  // 40% 길이의 보조선
                .background(color, RoundedCornerShape(1.dp))
        )
        if (!toLeft) Spacer(Modifier.weight(0.5f))   // 오른쪽에 빈 공간 절반
    }
}
