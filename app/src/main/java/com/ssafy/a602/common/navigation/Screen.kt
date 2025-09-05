package com.ssafy.a602.common.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object Game : Screen("game")
    object Chat : Screen("chat")
    object Learning : Screen("learning")
    object MyPage : Screen("mypage")
}