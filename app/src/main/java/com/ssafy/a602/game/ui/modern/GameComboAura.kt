package com.ssafy.a602.game.ui.modern

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.ssafy.a602.game.ui.GameUITheme
import kotlin.math.cos
import kotlin.math.sin

/**
 * 게임 콤보 오라 - 콤보에 따른 파티클 효과
 */
@Composable
fun GameComboAura(
    combo: Int,
    modifier: Modifier = Modifier
) {
    val auraLevel = when {
        combo >= 100 -> 3
        combo >= 50  -> 2
        combo >= 20  -> 1
        else -> 0
    }
    
    if (auraLevel == 0) return
    
    val infinite = rememberInfiniteTransition(label = "combo-aura")
    val pulse by infinite.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Canvas(modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val baseRadius = size.minDimension * 0.3f * pulse
        
        val (mainColor, secondaryColor, particleColor) = when (auraLevel) {
            1 -> Triple(
                GameUITheme.Colors.NeonBlue.copy(alpha = 0.05f),
                GameUITheme.Colors.NeonPurple.copy(alpha = 0.03f),
                GameUITheme.Colors.NeonBlue
            )
            2 -> Triple(
                GameUITheme.Colors.NeonPurple.copy(alpha = 0.08f),
                GameUITheme.Colors.NeonPink.copy(alpha = 0.05f),
                GameUITheme.Colors.NeonPurple
            )
            else -> Triple(
                GameUITheme.Colors.NeonGold.copy(alpha = 0.1f),
                GameUITheme.Colors.NeonLime.copy(alpha = 0.08f),
                GameUITheme.Colors.NeonGold
            )
        }
        
        // 메인 오라
        drawCircle(
            color = mainColor,
            radius = baseRadius,
            center = androidx.compose.ui.geometry.Offset(centerX, centerY)
        )
        
        // 보조 오라
        drawCircle(
            color = secondaryColor,
            radius = baseRadius * 1.5f,
            center = androidx.compose.ui.geometry.Offset(centerX, centerY),
            style = Stroke(width = 3f)
        )
        
        // 파티클 효과
        val particleCount = 12 + auraLevel * 8
        repeat(particleCount) { i ->
            val angle = (i / particleCount.toFloat()) * 2f * Math.PI + (pulse * Math.PI * 0.5)
            val radius = baseRadius * (1.3f + (i % 3) * 0.2f)
            val x = centerX + (radius * cos(angle)).toFloat()
            val y = centerY + (radius * sin(angle)).toFloat()
            
            drawCircle(
                color = particleColor.copy(alpha = 0.2f * pulse),
                radius = (1 + auraLevel).toFloat() * pulse,
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
        }
    }
}
