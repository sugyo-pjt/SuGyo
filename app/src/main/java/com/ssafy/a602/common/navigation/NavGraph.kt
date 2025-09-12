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
import com.ssafy.a602.learning.LearningMainPage
import com.ssafy.a602.learning.Total_RoadMap
import com.ssafy.a602.login.LoginScreen
import com.ssafy.a602.signup.SignUpScreen
@Composable
fun NavGraph(
    navController: NavHostController, // 화면 전환 컨트롤러
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,   // ✅ 로그인부터 시작
        modifier = modifier
    ) {
        /* ---------- Login ---------- */
        composable(Screen.Login.route) {
            // 뒤로가기 버튼은 보통 비활성/무시 처리. 필요 시 onBack = { navController.popBackStack() }
            com.ssafy.a602.login.LoginScreen(
                onBack = {}, // 로그인 첫화면이면 뒤로가기 동작 없음 권장
                onSubmit = { email, password ->
                    // TODO: 실제 인증 로직
                    navController.navigate(Screen.Home.route) {
                        // 로그인 화면을 스택에서 제거하여 뒤로가기로 못 돌아오게
                        popUpTo(Screen.Login.route) { inclusive = true }   // ✅ 핵심
                        launchSingleTop = true
                    }
                },
                onForgot = { /* navController.navigate("forgot") 등 필요 시 구현 */ },
                onSignup = { navController.navigate(Screen.Signup.route) }
            )
        }

        /* ---------- Signup ---------- */
        composable(Screen.Signup.route) {
            SignUpScreen(
                onBack = { navController.popBackStack() },
                onPickProfile = { /* 프로필 이미지 선택 로직 */ },
                onOpenTerms = { /* 이용약관 화면으로 이동 */ },
                onOpenPrivacy = { /* 개인정보처리방침 화면으로 이동 */ },
                onSubmit = { email, nickname, password, photo ->
                    // TODO: 실제 회원가입 로직
                    // 회원가입 성공 시 로그인 화면으로 이동
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Signup.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onLogin = { navController.popBackStack() }
            )
        }

        /* ---------- Home ---------- */
        composable(Screen.Home.route) {
            HomeScreen(
                onGoLearning = { navController.navigate(Screen.LearningMainPage.route) },
                onOpenChat = { navController.navigate(Screen.Chat.route) },
                onOpenGame = { navController.navigate(Screen.Game.route) },
                onOpenMyPage = { navController.navigate(Screen.MyPage.route) }
            )
        }

        composable(Screen.LearningMainPage.route) {
            LearningMainPage(
                // "로드맵 시작하기" 버튼 → 로드맵 화면으로 이동
                onStartRoadmap = { navController.navigate(Screen.Total_RoadMap.route) },
                progressDay = 5 // (가짜 값) 백엔드 값으로 교체 예정
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
