package com.ssafy.a602.common.navigation
// 라우터들 모음
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object Search : Screen("search")
    object Game : Screen("game")
    object Chat : Screen("chat")
    object LearningMainPage : Screen("learning")
    object MyPage : Screen("mypage")
    object Total_RoadMap   : Screen("learning/roadmap")
    
    // Game 관련 화면들
    object GameRanking : Screen("game_ranking")
    object GamePreparation : Screen("game_preparation")
    object GamePlay : Screen("game_play")
    object GameResult : Screen("game_result")
}