package com.ssafy.a602.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ========== Screen ========== */

@Composable
fun GamePlayScreen(
    songTitle: String,
    elapsedSec: Int,                 // 진행된 초
    totalSec: Int,                   // 총 길이(초)
    isPaused: Boolean,
    onTogglePause: () -> Unit,
    onEnd: () -> Unit,
    onOpenSettings: () -> Unit,
    // 가사/하이라이트는 간단 파라미터로 받되, 실제에선 ViewModel에서 내려주면 됨
    lyricTop: String,
    lyricMain: String,               // 하이라이트 단어 포함 라인
    highlightRange: IntRange? = null // 빨간 하이라이트 범위
) {
    var showPauseButton by remember { mutableStateOf(false) }
    val bg = Color(0xFF0D1118)
    val card = Color(0xFF151B24)
    val progress = Color(0xFF8B5CF6)   // 보라 진행바
    val greenBorder = Color(0xFF2BD46D)

    Surface(modifier = Modifier.fillMaxSize(), color = bg) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .statusBarsPadding()
            ) {
            /* Top bar */
            TopBarSection(
                title = songTitle,
                elapsedSec = elapsedSec,
                totalSec = totalSec,
                isPaused = isPaused,
                onTogglePause = onTogglePause,
                onOpenSettings = {
                    showPauseButton = !showPauseButton
                    onOpenSettings()
                },
                showPauseButton = showPauseButton
            )

            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { if (totalSec > 0) elapsedSec / totalSec.toFloat() else 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(8.dp)),
                trackColor = Color(0x33212535),
                color = progress
            )

            Spacer(Modifier.height(16.dp))

            /* Camera area */
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(card)
                    .border(3.dp, greenBorder, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Outlined.CameraAlt,
                        contentDescription = "카메라",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "수어 인식 카메라",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            /* Lyrics area */
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = card),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        lyricTop,
                        color = Color(0xFF9AA3B2),
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))

                    val body = buildAnnotatedString {
                        if (highlightRange != null &&
                            highlightRange.first in 0..lyricMain.lastIndex &&
                            highlightRange.last in 0..lyricMain.lastIndex &&
                            highlightRange.first <= highlightRange.last
                        ) {
                            append(lyricMain.substring(0, highlightRange.first))
                            withStyle(SpanStyle(color = Color(0xFFFF5A5A), fontWeight = FontWeight.Bold)) {
                                append(lyricMain.substring(highlightRange))
                            }
                        } else {
                            append(lyricMain)
                        }
                    }

                    Text(
                        body,
                        color = Color(0xFFE7ECF3),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(6.dp))
                    Text(
                        "우리가 함께 만들어가는",
                        color = Color(0xFF72809A),
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            /* Bottom button */
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                // 둥근 정사각형 종료 버튼
                Button(
                    onClick = onEnd,
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

            // 드롭다운 메뉴 오버레이 (설정 버튼 바로 아래)
            if (showPauseButton) {
                Card(
                    modifier = Modifier
                        .offset(x = 200.dp, y = 60.dp)
                        .width(140.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2329)),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Button(
                            onClick = onTogglePause,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA726)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            Icon(
                                Icons.Filled.Pause,
                                contentDescription = "일시정지",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "일시정지",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ========== Small Composables ========== */

@Composable
private fun TopBarSection(
    title: String,
    elapsedSec: Int,
    totalSec: Int,
    isPaused: Boolean,
    onTogglePause: () -> Unit,
    onOpenSettings: () -> Unit,
    showPauseButton: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = Color(0xFFE7ECF3),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.weight(1f)
        )
        Text(
            formatClock(elapsedSec),
            color = Color(0xFFE7ECF3),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.width(8.dp))

        // 설정 아이콘
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0x22212535))
        ) {
            Icon(Icons.Outlined.Settings, contentDescription = "설정", tint = Color(0xFFB8C2D6))
        }
    }
}

private fun formatClock(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return "%d:%02d".format(m, s)
}

/* ========== Preview ========== */

@Preview(showBackground = true, widthDp = 360, heightDp = 800, backgroundColor = 0xFF0D1118)
@Composable
private fun GamePlayScreenPreview() {
    var paused by remember { mutableStateOf(false) }
    GamePlayScreen(
        songTitle = "WAY BACK HOME",
        elapsedSec = if (paused) 0 else 17,
        totalSec = 180,
        isPaused = paused,
        onTogglePause = { paused = !paused },
        onEnd = {},
        onOpenSettings = {},
        lyricTop = "어떤 길을 걸어도",
        lyricMain = "열린 문을 향해 나아가",
        highlightRange = 0..2   // "열린" 하이라이트 예시
    )
}
