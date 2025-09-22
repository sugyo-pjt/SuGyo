package com.ssafy.a602.game.ui.modern

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.ssafy.a602.game.ui.GameUITheme

/**
 * 게임 카메라 프레임 - 콤보에 따른 네온 보더
 */
@Composable
fun GameCameraFrame(
    modifier: Modifier = Modifier,
    combo: Int,
    content: @Composable BoxScope.() -> Unit
) {
    val borderPulse = remember { Animatable(0f) }
    val comboGlow = remember { Animatable(0f) }
    
    LaunchedEffect(combo) {
        comboGlow.snapTo(1f)
        comboGlow.animateTo(0f, tween(600))
    }
    
    LaunchedEffect(Unit) {
        borderPulse.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000),
                repeatMode = RepeatMode.Reverse
            )
        )
    }
    
    val borderIntensity = (combo / 50f).coerceIn(0f, 1f)
    val glowAlpha = 0.6f + 0.3f * borderIntensity + 0.1f * borderPulse.value
    
    Box(
        modifier
            .background(GameUITheme.Colors.CardBackground)
            .border(
                width = 4.dp,
                brush = Brush.linearGradient(
                    colors = when {
                        combo >= 100 -> listOf(
                            GameUITheme.Colors.NeonGold.copy(glowAlpha),
                            GameUITheme.Colors.NeonLime.copy(glowAlpha)
                        )
                        combo >= 50 -> listOf(
                            GameUITheme.Colors.NeonPurple.copy(glowAlpha),
                            GameUITheme.Colors.NeonPink.copy(glowAlpha)
                        )
                        else -> listOf(
                            GameUITheme.Colors.NeonBlue.copy(glowAlpha),
                            GameUITheme.Colors.NeonPurple.copy(glowAlpha)
                        )
                    }
                ),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        content()
    }
}
