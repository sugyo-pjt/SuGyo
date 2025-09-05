package com.ssafy.a602.common.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ssafy.a602.home.HomeScreen
import com.ssafy.a602.game.GameScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) { 
            HomeScreen()
        }
        composable(Screen.Search.route) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "검색 화면",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        composable(Screen.Learning.route) { 
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "학습 화면",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        composable(Screen.Chat.route) { 
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "챗봇 화면",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        composable(Screen.Game.route) { 
            GameScreen()
        }
        composable(Screen.MyPage.route) { 
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "마이페이지 화면",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
