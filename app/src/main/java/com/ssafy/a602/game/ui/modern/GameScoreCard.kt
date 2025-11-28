package com.ssafy.a602.game.ui.modern

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.a602.game.ui.GameUITheme

/**
 * 게임 점수 카드 - 화려한 네온 효과
 */
@Composable
fun GameScoreCard(
    score: Int,
    grade: String,
    maxCombo: Int,
    modifier: Modifier = Modifier
) {
    val scoreScale = remember { Animatable(1f) }
    val gradeGlow = remember { Animatable(0f) }
    
    LaunchedEffect(score) {
        scoreScale.snapTo(1.1f)
        scoreScale.animateTo(1f, tween(200))
    }
    
    LaunchedEffect(grade) {
        gradeGlow.snapTo(1f)
        gradeGlow.animateTo(0f, tween(1000))
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = GameUITheme.Colors.CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            GameUITheme.Colors.CardBackground,
                            GameUITheme.Colors.NeonPurple.copy(alpha = 0.2f),
                            GameUITheme.Colors.CardBackground
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            GameUITheme.Colors.NeonBlue.copy(alpha = 0.5f),
                            GameUITheme.Colors.NeonPink.copy(alpha = 0.5f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Score
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "SCORE",
                        color = Color(0xFF9AA3B2),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$score",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.graphicsLayer {
                            scaleX = scoreScale.value
                            scaleY = scoreScale.value
                        }
                    )
                }
                
                // Grade
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "GRADE",
                        color = Color(0xFF9AA3B2),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        // 글로우 효과
                        if (gradeGlow.value > 0) {
                            Text(
                                grade,
                                color = GameUITheme.Colors.NeonGold.copy(alpha = 0.3f * gradeGlow.value),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        // 메인 텍스트
                        Text(
                            grade,
                            color = GameUITheme.Colors.NeonGold,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Max Combo
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "MAX COMBO",
                        color = Color(0xFF9AA3B2),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$maxCombo",
                        color = GameUITheme.Colors.NeonLime,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
