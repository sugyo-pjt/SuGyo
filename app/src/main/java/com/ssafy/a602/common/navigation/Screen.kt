package com.ssafy.a602.common.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object Game : Screen("game")
    object Chat : Screen("chat")
    object LearningMainPage : Screen("learning")
    object MyPage : Screen("mypage")
    object Total_RoadMap   : Screen("learning/roadmap")
}