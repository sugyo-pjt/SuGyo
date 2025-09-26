package com.ssafy.a602.game.play

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ssafy.a602.game.GameTheme
import com.ssafy.a602.game.data.GameMode

@Composable
fun TopBarSection(
    title: String,
    currentTime: Float,
    totalDuration: Float,
    isPaused: Boolean,
    onTogglePause: () -> Unit,
    onOpenSettings: () -> Unit = {}, // 기본값으로 빈 함수
    showPauseButton: Boolean = true, // 기본값을 true로 변경
    gameMode: GameMode? = null // 게임 모드 추가
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 노래 제목과 모드 표시
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                title,
                style = GameTheme.Typography.ScreenTitle
            )
            
            // 게임 모드 표시
            gameMode?.let { mode ->
                Spacer(Modifier.width(8.dp))
                Text(
                    text = mode.displayName,
                    style = GameTheme.Typography.CardTitle.copy(
                        color = when (mode) {
                            GameMode.EASY -> Color(0xFF10B981)
                            GameMode.HARD -> Color(0xFFEF4444)
                            GameMode.CHART_CREATION -> Color(0xFF8B5CF6)
                        }
                    )
                )
            }
        }
        Text(
            formatClock(currentTime.toInt()),
            style = GameTheme.Typography.CardTitle
        )
        Spacer(Modifier.width(8.dp))

        // 일시정지/재생 버튼
        if (showPauseButton) {
            IconButton(
                onClick = onTogglePause,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(GameTheme.Colors.CardBackground.copy(alpha = 0.2f))
            ) {
                Icon(
                    if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = if (isPaused) "재생" else "일시정지",
                    tint = GameTheme.Colors.TertiaryText
                )
            }
        }
    }
}

private fun formatClock(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return "%d:%02d".format(m, s)
}
