package com.ssafy.a602.common.navigation  // 패키지 경로(모듈/폴더 구조 상의 네임스페이스)

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
import com.ssafy.a602.game.SongsScreen

@Composable
fun NavGraph(
    navController: NavHostController, // 화면 전환을 수행하는 컨트롤러(상위에서 rememberNavController()로 생성)
    modifier: Modifier = Modifier     // 상위에서 전달받은 Modifier(여기선 통째로 NavHost에 전달)
) {
    NavHost(
        navController = navController,        // 어떤 컨트롤러로 그래프를 운용할지 지정
        startDestination = Screen.Home.route, // 앱 진입 시 처음 보여줄 라우트
        modifier = modifier                   // NavHost 자체에 Modifier 적용
    ) {
        // 각 라우트(route)와 실제 화면(컴포저블)을 연결
        composable(Screen.Home.route) {
            HomeScreen(
                onGoLearning = { navController.navigate(Screen.Learning.route) }, // ← 버튼 클릭 시 이동
                onOpenChat = { navController.navigate(Screen.Chat.route) },
                onOpenGame = { navController.navigate(Screen.Game.route) },
                onOpenMyPage = { navController.navigate(Screen.MyPage.route) }
            )  // 홈 라우트 진입 시 HomeScreen 컴포저블 표시
        }

        composable(Screen.Search.route) {
            // 임시(플레이스홀더) 검색 화면: 중앙 정렬된 텍스트만 배치
            Box(
                modifier = Modifier.fillMaxSize(),          // 화면 전체 채우기
                contentAlignment = Alignment.Center          // 자식(텍스트) 중앙 정렬
            ) {
                Text(
                    text = "검색 화면",                       // 표시할 문자열
                    fontSize = 24.sp,                        // 글자 크기
                    fontWeight = FontWeight.Bold             // 글자 굵게
                )
            }
        }

        composable(Screen.Learning.route) {
            // 임시 학습 화면
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
            // 임시 챗봇 화면
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
            SongsScreen()
        }

        composable(Screen.MyPage.route) {
            // 임시 마이페이지 화면
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
