package com.ssafy.a602.game.ui.modern

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.a602.game.play.JudgmentResult
import com.ssafy.a602.game.play.JudgmentType
import com.ssafy.a602.game.ui.GameUITheme

/**
 * 게임 판정 토스트 - 화면 중앙에서 폭발하는 효과
 */
@Composable
fun GameJudgmentToast(
    result: JudgmentResult?,
    modifier: Modifier = Modifier
) {
    val key = result?.type ?: return
    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }
    
    LaunchedEffect(key) {
        scale.snapTo(0.5f)
        alpha.snapTo(0f)
        rotation.snapTo(-10f)
        
        // 폭발 효과
        scale.animateTo(1.2f, tween(150))
        alpha.animateTo(1f, tween(100))
        rotation.animateTo(0f, tween(150))
        
        // 정착
        scale.animateTo(1f, tween(100))
        
        // 페이드 아웃
        alpha.animateTo(0f, tween(300, delayMillis = 500))
    }
    
    val (textColor, glowColor, bgColor) = when (result.type) {
        JudgmentType.PERFECT -> Triple(
            GameUITheme.Colors.Perfect,
            GameUITheme.Colors.PerfectGlow,
            GameUITheme.Colors.PerfectBg
        )
        JudgmentType.GREAT -> Triple(
            GameUITheme.Colors.Great,
            GameUITheme.Colors.GreatGlow,
            GameUITheme.Colors.GreatBg
        )
        JudgmentType.GOOD -> Triple(
            GameUITheme.Colors.Good,
            GameUITheme.Colors.GoodGlow,
            GameUITheme.Colors.GoodBg
        )
        JudgmentType.MISS -> Triple(
            GameUITheme.Colors.Miss,
            GameUITheme.Colors.MissGlow,
            GameUITheme.Colors.MissBg
        )
        else -> Triple(
            GameUITheme.Colors.Perfect, // 기본값: Perfect 색상
            GameUITheme.Colors.PerfectGlow,
            GameUITheme.Colors.PerfectBg
        )
    }
    
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(
            text = result.type.name,
            color = textColor,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 36.sp,
            modifier = Modifier
                .graphicsLayer {
                    this.scaleX = scale.value
                    this.scaleY = scale.value
                    this.alpha = alpha.value
                    this.rotationZ = rotation.value
                    this.shadowElevation = 20f
                }
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            bgColor,
                            Color.Transparent
                        ),
                        radius = 100f
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.6f),
                            Color.Transparent,
                            glowColor.copy(alpha = 0.6f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
