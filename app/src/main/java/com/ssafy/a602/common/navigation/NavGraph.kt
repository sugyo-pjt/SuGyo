package com.ssafy.a602.common.navigation  // 패키지 경로

// ── 필요한 것만 한 번씩만 import ─────────────────────────────────────────────
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
import com.ssafy.a602.learning.LearningMainPage
import com.ssafy.a602.learning.Total_RoadMap
import com.ssafy.a602.login.LoginScreen   // ✅ import했으니 아래에서 그냥 LoginScreen(...) 으로 호출

// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun NavGraph(
    navController: NavHostController, // 화면 전환 컨트롤러
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,   // ✅ 시작 화면: 로그인
        modifier = modifier
    ) {
        /* ---------- Login ---------- */
        composable(Screen.Login.route) {
            // 시작화면이라 뒤로가기는 무의미 → onBack = {} 그대로 두면 됨
            LoginScreen(
                onBack = {},
                onSubmit = { email, password ->
                    // TODO: 실제 인증 로직(성공 시 아래 네비게이션 실행)
                    navController.navigate(Screen.Home.route) {
                        // ✅ 로그인 화면을 백스택에서 제거 → 뒤로가기로 로그인 복귀 X
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onForgot = { /* navController.navigate("forgot") etc. */ },
                onSignup = { /* navController.navigate("signup") etc. */ }
            )
        }

        /* ---------- Home ---------- */
        composable(Screen.Home.route) {
            HomeScreen(
                onGoLearning = { navController.navigate(Screen.LearningMainPage.route) },
                onOpenChat   = { navController.navigate(Screen.Chat.route) },
                onOpenGame   = { navController.navigate(Screen.Game.route) },
                onOpenMyPage = { navController.navigate(Screen.MyPage.route) }
            )
        }

        /* ---------- Learning Main ---------- */
        composable(Screen.LearningMainPage.route) {
            LearningMainPage(
                onStartRoadmap = { navController.navigate(Screen.Total_RoadMap.route) },
                progressDay = 5 // TODO: 백엔드 값으로 교체
            )
        }

        /* ---------- Roadmap ---------- */
        composable(Screen.Total_RoadMap.route) {
            Total_RoadMap(
                onBack = { navController.popBackStack() },
                onDayClick = { /* day -> navController.navigate("lesson/$day") */ }
            )
        }

        /* ---------- Search ---------- */
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

        /* ---------- Chat ---------- */
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

        /* ---------- Game ---------- */
        composable(Screen.Game.route) {
            GameScreen()
        }

        /* ---------- MyPage ---------- */
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
