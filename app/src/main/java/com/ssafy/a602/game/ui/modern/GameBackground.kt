package com.ssafy.a602.game.ui.modern

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import com.ssafy.a602.game.ui.GameUITheme
import kotlin.math.cos
import kotlin.math.sin

/**
 * 게임 배경 - 비트에 반응하는 파동 효과
 */
@Composable
fun GameBackground(
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    combo: Int = 0
) {
    val pulse = remember { Animatable(0f) }
    
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            pulse.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            pulse.snapTo(0.3f)
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // 기본 그라데이션 배경 (매우 은은하게)
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GameUITheme.Colors.DarkBackground,
                            GameUITheme.Colors.NeonPurple.copy(alpha = 0.02f),
                            GameUITheme.Colors.DarkBackground
                        ),
                        radius = 800f
                    )
                )
        )
        
        // 비트 파동 효과
        Canvas(Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val maxRadius = size.minDimension * 0.4f
            
            // 콤보에 따른 색상 결정
            val (primaryColor, secondaryColor) = when {
                combo >= 100 -> Pair(GameUITheme.Colors.NeonGold, GameUITheme.Colors.NeonLime)
                combo >= 50 -> Pair(GameUITheme.Colors.NeonPurple, GameUITheme.Colors.NeonPink)
                combo >= 20 -> Pair(GameUITheme.Colors.NeonBlue, GameUITheme.Colors.NeonLime)
                else -> Pair(GameUITheme.Colors.NeonBlue, GameUITheme.Colors.NeonPink)
            }
            
            // 콤보 강도 계산 (전체 스코프에서 사용)
            val comboIntensity = (combo / 100f).coerceIn(0f, 1f)
            
            // 디지털 스타일의 파편화된 원형 파동 효과
            repeat(4) { i ->
                val wavePhase = (pulse.value + i * 0.25f) % 1f
                val waveRadius = maxRadius * (0.2f + 0.8f * wavePhase)
                val waveAlpha = (0.3f - 0.3f * wavePhase).coerceAtLeast(0f)
                
                // 콤보가 높을수록 더 강한 알파값
                val enhancedAlpha = waveAlpha * (1f + comboIntensity * 0.5f)
                
                // 점선 원형 파동 (파편화된 효과)
                val segments = 24 // 원을 24개 세그먼트로 나눔
                repeat(segments) { segment ->
                    val angle = (segment / segments.toFloat()) * 2f * Math.PI
                    val segmentLength = if (segment % 3 == 0) 0.8f else 0.4f // 일부 세그먼트는 짧게
                    
                    val startAngle = angle
                    val endAngle = angle + (segmentLength * 2f * Math.PI / segments.toFloat())
                    
                    // 메인 파동 (콤보에 따른 색상)
                    drawArc(
                        color = primaryColor.copy(alpha = enhancedAlpha),
                        startAngle = Math.toDegrees(startAngle.toDouble()).toFloat(),
                        sweepAngle = Math.toDegrees(endAngle - startAngle.toDouble()).toFloat(),
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(
                            centerX - waveRadius,
                            centerY - waveRadius
                        ),
                        size = androidx.compose.ui.geometry.Size(waveRadius * 2f, waveRadius * 2f),
                        style = Stroke(width = 2f + comboIntensity * 2f) // 콤보가 높을수록 더 두꺼운 선
                    )
                    
                    // 보조 파동 (더 파편화됨)
                    if (segment % 2 == 0) {
                        val outerRadius = waveRadius * 1.3f
                        drawArc(
                            color = secondaryColor.copy(alpha = enhancedAlpha * 0.6f),
                            startAngle = Math.toDegrees(startAngle.toDouble()).toFloat(),
                            sweepAngle = Math.toDegrees((endAngle - startAngle) * 0.5).toFloat(),
                            useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(
                                centerX - outerRadius,
                                centerY - outerRadius
                            ),
                            size = androidx.compose.ui.geometry.Size(outerRadius * 2f, outerRadius * 2f),
                            style = Stroke(width = 1.5f + comboIntensity * 1f)
                        )
                    }
                }
            }
            
            // 디지털 데이터 스트림 효과
            repeat(16) { i ->
                val angle = (i / 16f) * 2f * Math.PI + (pulse.value * Math.PI * 0.5f)
                val radius = maxRadius * (0.4f + 0.6f * pulse.value)
                val x = centerX + (radius * cos(angle)).toFloat()
                val y = centerY + (radius * sin(angle)).toFloat()
                
                // 데이터 포인트 (작은 사각형) - 콤보에 따른 색상
                val dataColor = when {
                    combo >= 100 -> GameUITheme.Colors.NeonGold
                    combo >= 50 -> GameUITheme.Colors.NeonPurple
                    combo >= 20 -> GameUITheme.Colors.NeonLime
                    else -> GameUITheme.Colors.NeonLime
                }
                
                drawRect(
                    color = dataColor.copy(alpha = 0.4f * pulse.value * (1f + comboIntensity * 0.5f)),
                    topLeft = androidx.compose.ui.geometry.Offset(x - 1f, y - 1f),
                    size = androidx.compose.ui.geometry.Size(2f + comboIntensity * 1f, 2f + comboIntensity * 1f)
                )
                
                // 연결선 (일부 포인트만) - 콤보에 따른 색상
                if (i % 3 == 0) {
                    val nextAngle = ((i + 1) / 16f) * 2f * Math.PI + (pulse.value * Math.PI * 0.5f)
                    val nextRadius = maxRadius * (0.4f + 0.6f * pulse.value)
                    val nextX = centerX + (nextRadius * cos(nextAngle)).toFloat()
                    val nextY = centerY + (nextRadius * sin(nextAngle)).toFloat()
                    
                    drawLine(
                        color = primaryColor.copy(alpha = 0.2f * pulse.value * (1f + comboIntensity * 0.3f)),
                        start = androidx.compose.ui.geometry.Offset(x, y),
                        end = androidx.compose.ui.geometry.Offset(nextX, nextY),
                        strokeWidth = 1f + comboIntensity * 0.5f
                    )
                }
            }
            
            // 중앙 글로우 효과 (콤보에 따른 색상)
            drawCircle(
                color = primaryColor.copy(alpha = 0.1f + 0.1f * pulse.value + comboIntensity * 0.2f),
                radius = maxRadius * 0.3f * (0.8f + 0.4f * pulse.value + comboIntensity * 0.3f),
                center = androidx.compose.ui.geometry.Offset(centerX, centerY)
            )
        }
    }
}
