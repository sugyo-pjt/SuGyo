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
                    animation = tween(800), // 애니메이션 속도 증가 (1000 -> 800)
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            pulse.snapTo(0.4f) // 정지 시에도 더 밝게
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // 기본 그라데이션 배경 (더 화려하게)
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GameUITheme.Colors.DarkBackground,
                            GameUITheme.Colors.NeonPurple.copy(alpha = 0.08f),
                            GameUITheme.Colors.NeonBlue.copy(alpha = 0.05f),
                            GameUITheme.Colors.DarkBackground
                        ),
                        radius = 1200f
                    )
                )
        )
        
        // 비트 파동 효과
        Canvas(Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val maxRadius = size.minDimension * 0.6f // 파동 크기 증가
            
            // 콤보에 따른 색상 결정 (더 화려한 색상 조합)
            val (primaryColor, secondaryColor) = when {
                combo >= 100 -> Pair(GameUITheme.Colors.NeonGold, GameUITheme.Colors.NeonCyan)
                combo >= 50 -> Pair(GameUITheme.Colors.NeonMagenta, GameUITheme.Colors.NeonOrange)
                combo >= 20 -> Pair(GameUITheme.Colors.NeonTurquoise, GameUITheme.Colors.NeonYellow)
                else -> Pair(GameUITheme.Colors.NeonBlue, GameUITheme.Colors.NeonPink)
            }
            
            // 콤보 강도 계산 (전체 스코프에서 사용)
            val comboIntensity = (combo / 100f).coerceIn(0f, 1f)
            
            // 디지털 스타일의 파편화된 원형 파동 효과 (더 화려하게)
            repeat(6) { i -> // 파동 개수 증가
                val wavePhase = (pulse.value + i * 0.2f) % 1f
                val waveRadius = maxRadius * (0.3f + 0.7f * wavePhase)
                val waveAlpha = (0.6f - 0.6f * wavePhase).coerceAtLeast(0f) // 알파값 증가
                
                // 콤보가 높을수록 더 강한 알파값
                val enhancedAlpha = waveAlpha * (1f + comboIntensity * 0.8f)
                
                // 점선 원형 파동 (파편화된 효과) - 더 화려하게
                val segments = 32 // 세그먼트 수 증가
                repeat(segments) { segment ->
                    val angle = (segment / segments.toFloat()) * 2f * Math.PI
                    val segmentLength = if (segment % 2 == 0) 0.9f else 0.5f // 세그먼트 길이 조정
                    
                    val startAngle = angle
                    val endAngle = angle + (segmentLength * 2f * Math.PI / segments.toFloat())
                    
                    // 메인 파동 (콤보에 따른 색상) - 더 두껍고 밝게
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
                        style = Stroke(width = 3f + comboIntensity * 3f) // 선 두께 증가
                    )
                    
                    // 보조 파동 (더 파편화됨) - 더 화려하게
                    if (segment % 2 == 0) {
                        val outerRadius = waveRadius * 1.5f // 외부 반지름 증가
                        drawArc(
                            color = secondaryColor.copy(alpha = enhancedAlpha * 0.8f), // 알파값 증가
                            startAngle = Math.toDegrees(startAngle.toDouble()).toFloat(),
                            sweepAngle = Math.toDegrees((endAngle - startAngle) * 0.7).toFloat(), // 각도 증가
                            useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(
                                centerX - outerRadius,
                                centerY - outerRadius
                            ),
                            size = androidx.compose.ui.geometry.Size(outerRadius * 2f, outerRadius * 2f),
                            style = Stroke(width = 2f + comboIntensity * 2f) // 선 두께 증가
                        )
                    }
                }
            }
            
            // 디지털 데이터 스트림 효과 - 더 화려하게
            repeat(24) { i -> // 데이터 포인트 수 증가
                val angle = (i / 24f) * 2f * Math.PI + (pulse.value * Math.PI * 0.8f)
                val radius = maxRadius * (0.5f + 0.5f * pulse.value)
                val x = centerX + (radius * cos(angle)).toFloat()
                val y = centerY + (radius * sin(angle)).toFloat()
                
                // 데이터 포인트 (작은 사각형) - 콤보에 따른 색상 (더 화려하게)
                val dataColor = when {
                    combo >= 100 -> GameUITheme.Colors.NeonGold
                    combo >= 50 -> GameUITheme.Colors.NeonMagenta
                    combo >= 20 -> GameUITheme.Colors.NeonTurquoise
                    else -> GameUITheme.Colors.NeonCyan
                }
                
                drawRect(
                    color = dataColor.copy(alpha = 0.7f * pulse.value * (1f + comboIntensity * 0.8f)), // 알파값 증가
                    topLeft = androidx.compose.ui.geometry.Offset(x - 2f, y - 2f), // 크기 증가
                    size = androidx.compose.ui.geometry.Size(4f + comboIntensity * 2f, 4f + comboIntensity * 2f) // 크기 증가
                )
                
                // 연결선 (일부 포인트만) - 콤보에 따른 색상, 더 화려하게
                if (i % 2 == 0) { // 연결선 빈도 증가
                    val nextAngle = ((i + 1) / 24f) * 2f * Math.PI + (pulse.value * Math.PI * 0.8f)
                    val nextRadius = maxRadius * (0.5f + 0.5f * pulse.value)
                    val nextX = centerX + (nextRadius * cos(nextAngle)).toFloat()
                    val nextY = centerY + (nextRadius * sin(nextAngle)).toFloat()
                    
                    drawLine(
                        color = primaryColor.copy(alpha = 0.4f * pulse.value * (1f + comboIntensity * 0.6f)), // 알파값 증가
                        start = androidx.compose.ui.geometry.Offset(x, y),
                        end = androidx.compose.ui.geometry.Offset(nextX, nextY),
                        strokeWidth = 2f + comboIntensity * 1f // 선 두께 증가
                    )
                }
            }
            
            // 중앙 글로우 효과 (콤보에 따른 색상) - 더 화려하게
            drawCircle(
                color = primaryColor.copy(alpha = 0.2f + 0.2f * pulse.value + comboIntensity * 0.4f), // 알파값 증가
                radius = maxRadius * 0.4f * (0.9f + 0.6f * pulse.value + comboIntensity * 0.5f), // 크기 증가
                center = androidx.compose.ui.geometry.Offset(centerX, centerY)
            )
            
            // 추가 글로우 효과 (더 화려한 시각 효과)
            drawCircle(
                color = secondaryColor.copy(alpha = 0.15f + 0.1f * pulse.value + comboIntensity * 0.3f),
                radius = maxRadius * 0.6f * (0.7f + 0.3f * pulse.value + comboIntensity * 0.4f),
                center = androidx.compose.ui.geometry.Offset(centerX, centerY)
            )
        }
    }
}
