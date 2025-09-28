package com.ssafy.a602.game.play

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.a602.game.play.JudgmentResult
import com.ssafy.a602.game.play.JudgmentType
import com.ssafy.a602.game.ui.GameUITheme

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
    
    // 판정별 색상 정의 (GameUITheme.Colors 사용)
    val judgmentColor = when (result.type) {
        JudgmentType.PERFECT -> GameUITheme.Colors.Perfect
        JudgmentType.GOOD -> GameUITheme.Colors.Good
        JudgmentType.MISS -> GameUITheme.Colors.Miss
        else -> GameUITheme.Colors.Miss
    }
    
    val judgmentText = when (result.type) {
        JudgmentType.PERFECT -> "PERFECT"
        JudgmentType.GOOD -> "GOOD"
        JudgmentType.MISS -> "MISS"
        else -> "UNKNOWN"
    }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 배경 블러 효과 (은은한 색상)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(judgmentColor.copy(alpha = 0.1f))
                .blur(20.dp)
        )
        
        // 메인 판정 텍스트
        Text(
            text = judgmentText,
            color = judgmentColor,
            fontSize = 55.sp, // 글자 크기 55.sp로 변경
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier
                .alpha(alpha)
                .scale(scale)
        )
        
        // 유사도 표시 (HARD 모드에서만)
        if (result.accuracy > 0f) {
            Text(
                text = "${(result.accuracy * 100).toInt()}%",
                color = judgmentColor.copy(alpha = 0.8f),
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .alpha(alpha * 0.8f)
                    .scale(scale * 0.8f)
            )
        }
    }
}
