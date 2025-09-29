package com.ssafy.a602.game.ui.modern

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ssafy.a602.game.ui.GameUITheme

/**
 * 게임 진행바 - 글로우 효과
 */
@Composable
fun GameProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val glow = remember { Animatable(0f) }
    
    LaunchedEffect(progress) {
        glow.snapTo(1f)
        glow.animateTo(0f, tween(800))
    }
    
    Box(modifier = modifier) {
        // 기본 진행바
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(10.dp)),
            trackColor = Color(0x33212535),
            color = GameUITheme.Colors.NeonBlue.copy(alpha = 0.8f)
        )
        
        // 글로우 효과
        Canvas(Modifier.fillMaxSize()) {
            val glowAlpha = 0.2f * glow.value
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        GameUITheme.Colors.NeonBlue.copy(alpha = glowAlpha),
                        Color.Transparent
                    )
                )
            )
        }
    }
}
