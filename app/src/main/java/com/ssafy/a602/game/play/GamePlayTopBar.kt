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
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ssafy.a602.game.GameTheme

@Composable
fun TopBarSection(
    title: String,
    currentTime: Float,
    totalDuration: Float,
    isPaused: Boolean,
    onTogglePause: () -> Unit,
    onOpenSettings: () -> Unit,
    showPauseButton: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = GameTheme.Typography.ScreenTitle,
            modifier = Modifier.weight(1f)
        )
        Text(
            formatClock(currentTime.toInt()),
            style = GameTheme.Typography.CardTitle
        )
        Spacer(Modifier.width(8.dp))

        // 설정 아이콘
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(GameTheme.Colors.CardBackground.copy(alpha = 0.2f))
        ) {
            Icon(
                Icons.Outlined.Settings,
                contentDescription = "설정",
                tint = GameTheme.Colors.TertiaryText
            )
        }
    }
}

private fun formatClock(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return "%d:%02d".format(m, s)
}
