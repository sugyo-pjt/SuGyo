package com.ssafy.a602.game.play

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ssafy.a602.game.play.JudgmentResult
import com.ssafy.a602.game.play.JudgmentType

@Composable
fun JudgmentOverlay(
    result: JudgmentResult
) {
    // Preview에서도 잘 보이도록 단순한 애니메이션으로 변경
    val infiniteTransition = rememberInfiniteTransition(label = "judgment")
    
    // 페이드 인/아웃 애니메이션 (더 빠르게)
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    // 스케일 애니메이션 (더 부드럽게)
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when (result.type) {
                JudgmentType.PERFECT -> "PERFECT"
                JudgmentType.MISS -> "MISS"
                else -> "UNKNOWN"
            },
            color = when (result.type) {
                JudgmentType.PERFECT -> Color(0xFF3B82F6) // 파란색
                JudgmentType.MISS -> Color(0xFFFF5A5A) // 빨간색
                else -> Color(0xFF808080) // 회색
            },
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier
                .alpha(alpha)
                .scale(scale)
        )
    }
}
