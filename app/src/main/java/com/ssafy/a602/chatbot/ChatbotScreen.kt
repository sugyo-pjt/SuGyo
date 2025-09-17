package com.ssafy.a602.chatbot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

/* ───────────────────────── 메인 화면 ───────────────────────── */

@Composable
fun ChatbotScreen(
    onBack: () -> Unit = {}
) {
    val bg = Brush.verticalGradient(listOf(Color(0xFFF2F6FF), Color.White))

    // ★ 가짜 백엔드 → 나중에 Real 로 교체(예: RealChatApi(...))
    val vm: ChatbotViewModel = viewModel(
        factory = ChatbotViewModel.Factory(backend = ChatFakeApi())
    )

    var input by remember { mutableStateOf("") }

    Box(
        Modifier.fillMaxSize().background(bg).padding(horizontal = 16.dp)
    ) {
        Column(Modifier.fillMaxSize()) {

            /* ── 상단바 ── */
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("←",
                    fontSize = 20.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onBack() }
                        .padding(4.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "AI 수어 챗봇",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.weight(1f))
                ModeToggle(
                    mode = vm.mode.value,
                    onChange = vm::changeMode
                )
            }

            Spacer(Modifier.height(10.dp))

            /* ── 시나리오 선택(학습 모드만) ── */
            if (vm.mode.value == BotMode.LEARN) {
                Text("시나리오 선택", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    vm.scenarios.forEach { sc ->
                        val selected = sc.id == vm.selectedScenario.value?.id
                        val shape = RoundedCornerShape(10.dp)
                        AssistChip(
                            modifier = Modifier
                                .clip(shape)
                                .then(
                                    if (selected) Modifier.border(1.dp, Color(0xFF1D4ED8), shape)
                                    else Modifier
                                ),
                            label = { Text(sc.title) },
                            onClick = { vm.selectScenario(sc) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (selected) Color(0xFFE8EEFF) else Color(0xFFF5F7FA),
                                labelColor     = if (selected) Color(0xFF1D4ED8) else Color(0xFF111827)
                            )
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            /* ── 메시지 ── */
            MessagesPanel(messages = vm.messages)
            Spacer(Modifier.height(10.dp))

            /* ── 좌표 수신 상태 ── */
            if (vm.receiving.value) {
                StatusPill("좌표 수신 중…")
                Spacer(Modifier.height(8.dp))
            }

            /* ── 손 좌표 렌더(2D) ── */
            HandCanvas(
                frame = vm.currentFrame.value,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(16.dp))
                    .padding(8.dp)
            )

            Spacer(Modifier.height(10.dp))

            /* ── 입력창 ── */
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = {
                        Text(if (vm.mode.value == BotMode.FREE) "텍스트 또는 수어 설명"
                        else "시나리오에 맞게 답변을 입력해보세요")
                    },
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    maxLines = 3
                )
                Button(
                    onClick = {
                        val txt = input.trim()
                        input = ""
                        vm.sendUserText(txt)
                    },
                    shape = RoundedCornerShape(12.dp),
                    enabled = vm.mode.value == BotMode.FREE || vm.selectedScenario.value != null
                ) { Text("전송") }
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}

/* ───────────────────── UI 조각들 ───────────────────── */

@Composable
private fun ModeToggle(
    mode: BotMode,
    onChange: (BotMode) -> Unit
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
        @Composable
        fun Tab(text: String, selected: Boolean, onClick: () -> Unit) {
            Text(
                text = text,
                color = if (selected) Color.White else blue,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) blue else Color.Transparent)
                    .clickable { onClick() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        Tab("학습 모드", mode == BotMode.LEARN) { onChange(BotMode.LEARN) }
        Tab("자유 모드",  mode == BotMode.FREE)  { onChange(BotMode.FREE)  }
    }
}

@Composable
private fun MessagesPanel(messages: List<ChatMessage>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        messages.forEach { m ->
            val bubbleColor = if (m.isBot) Color(0xFFEFF4FF) else Color(0xFFE7F7EC)
            val textColor   = if (m.isBot) Color(0xFF1D4ED8) else Color(0xFF166534)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = if (m.isBot) Arrangement.Start else Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(bubbleColor)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .widthIn(max = 280.dp)
                ) { Text(m.text, color = textColor) }
            }
        }
    }
}

@Composable
private fun StatusPill(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = Color(0xFF6B7280)) }
}

/** 손 21포인트를 캔버스에 라인 + 포인트로 그리는 간단한 2D 렌더러 */
@Composable
private fun HandCanvas(
    frame: HandFrame?,
    modifier: Modifier = Modifier
) {
    val edges = remember {
        // 대략적인 손 연결 (Mediapipe 스타일)
        listOf(
            0 to 5, 5 to 9, 9 to 13, 13 to 17, 17 to 0,          // 손바닥
            1 to 2, 2 to 3, 3 to 4,                              // 엄지
            5 to 6, 6 to 7, 7 to 8,                              // 검지
            9 to 10, 10 to 11, 11 to 12,                         // 중지
            13 to 14, 14 to 15, 15 to 16,                        // 약지
            17 to 18, 18 to 19, 19 to 20                         // 새끼
        )
    }
    Canvas(modifier = modifier) {
        val lm = frame?.landmarks ?: return@Canvas
        fun map(p: Pt) = Offset(p.x * size.width, p.y * size.height)

        // 라인
        edges.forEach { (a, b) ->
            if (a in lm.indices && b in lm.indices) {
                drawLine(
                    color = Color(0xFF2563EB),
                    start = map(lm[a]),
                    end   = map(lm[b]),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
            }
        }
        // 포인트
        lm.forEach { p ->
            drawCircle(
                color = Color(0xFF10B981),
                radius = 6f,
                center = map(p),
                style  = Stroke(width = 6f)
            )
        }
    }
}
