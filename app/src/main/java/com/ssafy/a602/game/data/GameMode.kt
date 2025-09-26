package com.ssafy.a602.game.data

/**
 * 게임 모드 enum
 */
enum class GameMode(val displayName: String, val description: String) {
    EASY("Easy", "HTTP 방식으로 데이터 전송"),
    HARD("Hard", "웹소켓 방식으로 실시간 전송"),
    CHART_CREATION("채보만들기", "채보 제작 모드 - HTTP로 저장")
}
