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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.a602.game.ui.GameUITheme

/**
 * 게임 가사 카드 - 3소절 표시 (이전, 현재, 다음)
 */
@Composable
fun GameLyricsCard(
    previousLyric: String?,
    currentLyric: String?,
    nextLyric: String?,
    progress: Float = 0f,
    modifier: Modifier = Modifier
) {
    val textGlow = remember { Animatable(0f) }
    
    // 디버깅 로그
    LaunchedEffect(previousLyric, currentLyric, nextLyric, progress) {
        android.util.Log.d("GameLyricsCard", "가사 카드 상태: previous='$previousLyric', current='$currentLyric', next='$nextLyric', progress=$progress")
    }
    
    LaunchedEffect(currentLyric) {
        textGlow.snapTo(1f)
        textGlow.animateTo(0f, tween(800))
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GameUITheme.Colors.CardBackground),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            GameUITheme.Colors.CardBackground,
                            GameUITheme.Colors.NeonPurple.copy(alpha = 0.15f),
                            GameUITheme.Colors.CardBackground
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            GameUITheme.Colors.NeonBlue.copy(alpha = 0.3f),
                            GameUITheme.Colors.NeonPink.copy(alpha = 0.3f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 이전 가사
                if (!previousLyric.isNullOrBlank()) {
                    Text(
                        text = previousLyric,
                        color = Color(0xFF7C8799),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.graphicsLayer {
                            alpha = 0.7f
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                }
                
                // 현재 가사 (메인)
                if (!currentLyric.isNullOrBlank()) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        // 진행바
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(50)),
                            color = GameUITheme.Colors.NeonBlue.copy(alpha = 0.8f),
                            trackColor = Color(0x33212535)
                        )
                        
                        // 그라데이션 텍스트
                        Text(
                            text = currentLyric,
                            textAlign = TextAlign.Center,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.graphicsLayer {
                                scaleX = 1f + 0.05f * textGlow.value
                                scaleY = 1f + 0.05f * textGlow.value
                            },
                            style = TextStyle(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        GameUITheme.Colors.NeonPink.copy(alpha = 0.9f),
                                        GameUITheme.Colors.NeonBlue.copy(alpha = 0.9f),
                                        GameUITheme.Colors.NeonLime.copy(alpha = 0.9f)
                                    )
                                )
                            )
                        )
                    }
                } else {
                    Text(
                        text = "곡을 준비하고 있습니다",
                        color = Color(0xFF9AA3B2),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
                
                // 다음 가사
                if (!nextLyric.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = nextLyric,
                        color = Color(0xFF6B7280),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.graphicsLayer {
                            alpha = 0.6f
                        }
                    )
                }
            }
        }
    }
}
