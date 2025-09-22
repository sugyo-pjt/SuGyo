package com.ssafy.a602.game.ui.modern

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.a602.game.play.JudgmentResult
import com.ssafy.a602.game.play.JudgmentType
import com.ssafy.a602.game.ui.GameUITheme

/**
 * 판정 토스트 애니메이션 데모 - 각 판정별 애니메이션을 확인할 수 있는 프리뷰
 */
@Composable
fun GameJudgmentToastDemo() {
    var currentJudgment by remember { mutableStateOf<JudgmentType?>(null) }
    var isAnimating by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GameUITheme.Colors.DarkBackground)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "판정 토스트 애니메이션 데모",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(Modifier.height(32.dp))
        
        // 판정 버튼들
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    currentJudgment = JudgmentType.PERFECT
                    isAnimating = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = GameUITheme.Colors.Perfect
                )
            ) {
                Text("PERFECT", color = Color.White)
            }
            
            Button(
                onClick = {
                    currentJudgment = JudgmentType.GREAT
                    isAnimating = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = GameUITheme.Colors.Great
                )
            ) {
                Text("GREAT", color = Color.White)
            }
            
            Button(
                onClick = {
                    currentJudgment = JudgmentType.GOOD
                    isAnimating = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = GameUITheme.Colors.Good
                )
            ) {
                Text("GOOD", color = Color.White)
            }
            
            Button(
                onClick = {
                    currentJudgment = JudgmentType.MISS
                    isAnimating = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = GameUITheme.Colors.Miss
                )
            ) {
                Text("MISS", color = Color.White)
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        // 판정 토스트 표시 영역
        Box(
            modifier = Modifier
                .size(400.dp, 200.dp)
                .background(
                    color = Color(0xFF1A1F2E),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // 판정 토스트
            if (currentJudgment != null) {
                val judgmentResult = JudgmentResult(
                    type = currentJudgment!!,
                    accuracy = when (currentJudgment) {
                        JudgmentType.PERFECT -> 0.98f
                        JudgmentType.GREAT -> 0.85f
                        JudgmentType.GOOD -> 0.70f
                        JudgmentType.MISS -> 0.0f
                        else -> 0.0f
                    },
                    score = when (currentJudgment) {
                        JudgmentType.PERFECT -> 1000
                        JudgmentType.GREAT -> 800
                        JudgmentType.GOOD -> 500
                        JudgmentType.MISS -> 0
                        else -> 0
                    },
                    combo = 50,
                    timestamp = System.currentTimeMillis()
                )
                
                GameJudgmentToast(
                    result = judgmentResult,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = "버튼을 눌러서\n판정 애니메이션을 확인하세요!",
                    color = Color(0xFF6B7280),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        // 현재 판정 정보 표시
        if (currentJudgment != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2A2F3E)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "현재 판정: ${currentJudgment!!.name}",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    val (textColor, glowColor, bgColor) = when (currentJudgment) {
                        JudgmentType.PERFECT -> Triple(
                            GameUITheme.Colors.Perfect,
                            GameUITheme.Colors.PerfectGlow,
                            "파란색 기반"
                        )
                        JudgmentType.GREAT -> Triple(
                            GameUITheme.Colors.Great,
                            GameUITheme.Colors.GreatGlow,
                            "주황색 기반"
                        )
                        JudgmentType.GOOD -> Triple(
                            GameUITheme.Colors.Good,
                            GameUITheme.Colors.GoodGlow,
                            "노란색 기반"
                        )
                        JudgmentType.MISS -> Triple(
                            GameUITheme.Colors.Miss,
                            GameUITheme.Colors.MissGlow,
                            "빨간색 기반"
                        )
                        else -> Triple(Color.White, Color.White, "기본")
                    }
                    
                    Text(
                        text = "색상: $bgColor",
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = "글로우: ${glowColor.toString().substringAfter("Color(").substringBefore(")")}",
                        color = glowColor,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // 애니메이션 설명
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1F2E)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "애니메이션 단계:",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall
                )
                
                Spacer(Modifier.height(8.dp))
                
                Text(
                    text = "1. 폭발 효과 (150ms): 0.5배 → 1.2배 스케일",
                    color = Color(0xFF9AA3B2),
                    style = MaterialTheme.typography.bodySmall
                )
                
                Text(
                    text = "2. 정착 (100ms): 1.2배 → 1.0배 스케일",
                    color = Color(0xFF9AA3B2),
                    style = MaterialTheme.typography.bodySmall
                )
                
                Text(
                    text = "3. 페이드 아웃 (300ms, 500ms 지연): 알파 1.0 → 0.0",
                    color = Color(0xFF9AA3B2),
                    style = MaterialTheme.typography.bodySmall
                )
                
                Text(
                    text = "4. 회전 효과: -10도 → 0도 (150ms)",
                    color = Color(0xFF9AA3B2),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 400,
    heightDp = 800,
    backgroundColor = 0xFF0B0E13
)
@Composable
fun GameJudgmentToastDemoPreview() {
    GameJudgmentToastDemo()
}

@Preview(
    showBackground = true,
    widthDp = 400,
    heightDp = 800,
    backgroundColor = 0xFF0B0E13
)
@Composable
fun GameJudgmentToastDemoPerfect() {
    var currentJudgment by remember { mutableStateOf(JudgmentType.PERFECT) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GameUITheme.Colors.DarkBackground)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "PERFECT 판정 데모",
            color = GameUITheme.Colors.Perfect,
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(Modifier.height(32.dp))
        
        Box(
            modifier = Modifier
                .size(300.dp, 150.dp)
                .background(
                    color = Color(0xFF1A1F2E),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            val judgmentResult = JudgmentResult(
                type = JudgmentType.PERFECT,
                accuracy = 0.98f,
                score = 1000,
                combo = 50,
                timestamp = System.currentTimeMillis()
            )
            
            GameJudgmentToast(
                result = judgmentResult,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 400,
    heightDp = 800,
    backgroundColor = 0xFF0B0E13
)
@Composable
fun GameJudgmentToastDemoGreat() {
    var currentJudgment by remember { mutableStateOf(JudgmentType.GREAT) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GameUITheme.Colors.DarkBackground)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "GREAT 판정 데모",
            color = GameUITheme.Colors.Great,
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(Modifier.height(32.dp))
        
        Box(
            modifier = Modifier
                .size(300.dp, 150.dp)
                .background(
                    color = Color(0xFF1A1F2E),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            val judgmentResult = JudgmentResult(
                type = JudgmentType.GREAT,
                accuracy = 0.85f,
                score = 800,
                combo = 30,
                timestamp = System.currentTimeMillis()
            )
            
            GameJudgmentToast(
                result = judgmentResult,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 400,
    heightDp = 800,
    backgroundColor = 0xFF0B0E13
)
@Composable
fun GameJudgmentToastDemoGood() {
    var currentJudgment by remember { mutableStateOf(JudgmentType.GOOD) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GameUITheme.Colors.DarkBackground)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "GOOD 판정 데모",
            color = GameUITheme.Colors.Good,
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(Modifier.height(32.dp))
        
        Box(
            modifier = Modifier
                .size(300.dp, 150.dp)
                .background(
                    color = Color(0xFF1A1F2E),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            val judgmentResult = JudgmentResult(
                type = JudgmentType.GOOD,
                accuracy = 0.70f,
                score = 500,
                combo = 15,
                timestamp = System.currentTimeMillis()
            )
            
            GameJudgmentToast(
                result = judgmentResult,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 400,
    heightDp = 800,
    backgroundColor = 0xFF0B0E13
)
@Composable
fun GameJudgmentToastDemoMiss() {
    var currentJudgment by remember { mutableStateOf(JudgmentType.MISS) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GameUITheme.Colors.DarkBackground)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "MISS 판정 데모",
            color = GameUITheme.Colors.Miss,
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(Modifier.height(32.dp))
        
        Box(
            modifier = Modifier
                .size(300.dp, 150.dp)
                .background(
                    color = Color(0xFF1A1F2E),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            val judgmentResult = JudgmentResult(
                type = JudgmentType.MISS,
                accuracy = 0.0f,
                score = 0,
                combo = 0,
                timestamp = System.currentTimeMillis()
            )
            
            GameJudgmentToast(
                result = judgmentResult,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
