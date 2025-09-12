package com.ssafy.a602.game

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 게임 패키지 통일된 디자인 시스템
 */
object GameTheme {
    
    // ===== 색상 팔레트 =====
    object Colors {
        // 배경
        val BackgroundGradient = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF3D2C8D), // 보라
                Color(0xFF1F2A6B), // 남색
                Color(0xFF0E2149)  // 진한 남색
            )
        )
        
        val DarkBackground = Color(0xFF0D1118)
        val CardBackground = Color(0xFF1B2454)
        val DarkCard = Color(0xFF151B24)
        
        // 텍스트
        val PrimaryText = Color(0xFFE7ECF3)      // 주요 텍스트 (제목 등)
        val SecondaryText = Color.White          // 보조 텍스트
        val TertiaryText = Color(0xFF9AA3B2)     // 3차 텍스트 (설명 등)
        val MutedText = Color(0xFF6B7280)        // 흐린 텍스트
        
        // 액센트 색상
        val Progress = Color(0xFF8B5CF6)         // 진행바
        val GreenBorder = Color(0xFF2BD46D)      // 녹색 테두리
        val AccuracyBlue = Color(0xFF7BB8FF)     // 정확도 파란색
        val ComboYellow = Color(0xFFFFD166)      // 콤보 노란색
        val PerfectBar = Color(0xFF6BB7FF)       // Perfect 바
        val MissBar = Color(0xFFFF6B6B)          // Miss 바
        val ErrorRed = Color(0xFFFF5A5A)         // 에러/경고 빨간색
        
        // 버튼 색상
        val PrimaryButton = Color(0xFF3772FF)    // 주요 버튼
        val SecondaryButton = Color(0xFFF5A524)  // 보조 버튼
        val DangerButton = Color(0xFFFF5A5A)     // 위험 버튼
        val SuccessButton = Color(0xFF4CAF50)    // 성공 버튼
        val WarningButton = Color(0xFFFFA726)    // 경고 버튼
    }
    
    // ===== 텍스트 스타일 =====
    object Typography {
        val ScreenTitle = TextStyle(
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = Colors.PrimaryText
        )
        
        val SectionTitle = TextStyle(
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Colors.PrimaryText
        )
        
        val CardTitle = TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Colors.PrimaryText
        )
        
        val BodyText = TextStyle(
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = Colors.PrimaryText
        )
        
        val SecondaryText = TextStyle(
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = Colors.TertiaryText
        )
        
        val CaptionText = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = Colors.MutedText
        )
        
        val LargeNumber = TextStyle(
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Colors.PrimaryText
        )
        
        val MediumNumber = TextStyle(
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Colors.PrimaryText
        )
    }
    
    // ===== 간격 시스템 =====
    object Spacing {
        val xs = 4.dp
        val sm = 8.dp
        val md = 12.dp
        val lg = 16.dp
        val xl = 20.dp
        val xxl = 24.dp
        val xxxl = 32.dp
    }
    
    // ===== 둥근 모서리 =====
    object CornerRadius {
        val sm = 8.dp
        val md = 12.dp
        val lg = 16.dp
        val xl = 20.dp
        val xxl = 24.dp
    }
    
    // ===== 카드 패딩 =====
    object CardPadding {
        val sm = Spacing.sm
        val md = Spacing.lg
        val lg = Spacing.xl
    }
}
