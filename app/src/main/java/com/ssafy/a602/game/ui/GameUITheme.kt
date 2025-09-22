package com.ssafy.a602.game.ui

import androidx.compose.ui.graphics.Color

/**
 * 게임 UI용 네온 테마
 */
object GameUITheme {
    
    // 네온 색상 팔레트
    object Colors {
        val NeonPink = Color(0xFFFF4DD2)
        val NeonBlue = Color(0xFF48E0FF)
        val NeonPurple = Color(0xFF9A7BFF)
        val NeonLime = Color(0xFFB6FF3B)
        val NeonGold = Color(0xFFFFD700)
        val NeonRed = Color(0xFFFF5A5A)
        
        val DarkBackground = Color(0xFF0B0E13)
        val CardBackground = Color(0xFF1A1F2E)
        val BorderColor = Color(0xFF2A2F3E)
        
        // 텍스트 색상
        val PrimaryText = Color(0xFFE7ECF3)
        val SecondaryText = Color(0xFF9AA3B2)
        
        // 판정별 색상 (새로운 색상 체계)
        val Perfect = Color(0xFF4A90E2) // 파란색 기반
        val PerfectGlow = Color(0xFF87CEEB) // 하늘색 글로우
        val PerfectBg = Color(0xFF4A90E2).copy(alpha = 0.1f) // 파란색 배경
        
        val Great = Color(0xFFFF8C00) // 주황색 기반
        val GreatGlow = Color(0xFFFFA500) // 오렌지 글로우
        val GreatBg = Color(0xFFFF8C00).copy(alpha = 0.1f) // 주황색 배경
        
        val Good = Color(0xFFFFD700) // 노란색 기반
        val GoodGlow = Color(0xFFFFF8DC) // 크림색 글로우
        val GoodBg = Color(0xFFFFD700).copy(alpha = 0.1f) // 노란색 배경
        
        val Miss = Color(0xFFFF5A5A) // 빨간색 기반
        val MissGlow = Color(0xFFFF6B6B) // 밝은 빨간색 글로우
        val MissBg = Color(0xFFFF5A5A).copy(alpha = 0.1f) // 빨간색 배경
        
        // 콤보별 색상
        val ComboLow = Color(0xFF48E0FF)
        val ComboMid = Color(0xFF9A7BFF)
        val ComboHigh = Color(0xFFFFD700)
    }
}
