package com.ssafy.a602.common.navigation  // 패키지 경로(모듈/폴더 구조 상의 네임스페이스)

import androidx.compose.foundation.layout.Box          // 레이아웃 컨테이너(겹쳐 배치/정렬에 유용)
import androidx.compose.foundation.layout.fillMaxSize // 부모 사이즈를 가득 채우는 Modifier
import androidx.compose.material3.Text                // 글자 표시용 컴포저블
import androidx.compose.runtime.Composable            // 컴포저블 함수임을 표시하는 어노테이션
import androidx.compose.ui.Alignment                 // Box 등에서 자식 정렬할 때 사용
import androidx.compose.ui.Modifier                  // 컴포저블에 체이닝으로 속성 부여(크기/패딩 등)
import androidx.compose.ui.text.font.FontWeight      // 글자 두께(굵기)
import androidx.compose.ui.unit.sp                   // 글자 크기 단위
import androidx.navigation.NavHostController         // 네비게이션을 실제로 조종하는 컨트롤러
import androidx.navigation.compose.NavHost           // 라우트 그래프를 담는 컨테이너
import androidx.navigation.compose.composable        // 특정 라우트에 컴포저블 화면을 연결
import com.ssafy.a602.home.HomeScreen                // 홈 화면(별도 파일에서 구현)
import com.ssafy.a602.game.GameScreen                // 게임 화면(별도 파일에서 구현)
import com.ssafy.a602.learning.LearningMainPage
import com.ssafy.a602.learning.Total_RoadMap

import androidx.compose.foundation.layout.Box          // 레이아웃 컨테이너(겹쳐 배치/정렬에 유용)
import androidx.compose.foundation.layout.fillMaxSize // 부모 사이즈를 가득 채우는 Modifier
import androidx.compose.material3.Text                // 글자 표시용 컴포저블
import androidx.compose.runtime.Composable            // 컴포저블 함수임을 표시하는 어노테이션
import androidx.compose.ui.Alignment                 // Box 등에서 자식 정렬할 때 사용
import androidx.compose.ui.Modifier                  // 컴포저블에 체이닝으로 속성 부여(크기/패딩 등)
import androidx.compose.ui.text.font.FontWeight      // 글자 두께(굵기)
import androidx.compose.ui.unit.sp                   // 글자 크기 단위
import androidx.navigation.NavHostController         // 네비게이션을 실제로 조종하는 컨트롤러
import androidx.navigation.compose.NavHost           // 라우트 그래프를 담는 컨테이너
import androidx.navigation.compose.composable        // 특정 라우트에 컴포저블 화면을 연결
import com.ssafy.a602.home.HomeScreen                // 홈 화면(별도 파일에서 구현)
import com.ssafy.a602.game.GameScreen                // 게임 화면(별도 파일에서 구현)
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
                onGoLearning = { navController.navigate(Screen.LearningMainPage.route) }, // ← 버튼 클릭 시 이동
                onOpenChat = { navController.navigate(Screen.Chat.route) },
                onOpenGame = { navController.navigate(Screen.Game.route) },
                onOpenMyPage = { navController.navigate(Screen.MyPage.route) }
            )  // 홈 라우트 진입 시 HomeScreen 컴포저블 표시
        }

        composable(Screen.LearningMainPage.route) {
            LearningMainPage(
                // "로드맵 시작하기" 버튼 → 로드맵 화면으로 이동
                onStartRoadmap = { navController.navigate(Screen.Total_RoadMap.route) },
                progressDay = 5 // (가짜 값) 백엔드 값으로 교체 예정
            )
        }

        composable(Screen.Total_RoadMap.route) {
            Total_RoadMap(
                onBack = { navController.popBackStack() },
                onDayClick = { day -> /* navController.navigate("lesson/$day") */ }
            )
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

//        composable(Screen.LearningMainPage.route) {
//            LearningMainPage(
//                onStartRoadmap = {
//                    // ⬇️ [추가] "로드맵 시작하기" 버튼 클릭 시 로드맵 화면으로 이동
//                    navController.navigate(Screen.Total_RoadMap.route)
//                },
//                progressDay = 5
//            )
//        }

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
            GameScreen()  // 게임 라우트 진입 시 GameScreen 표시
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

//        composable(Screen.Total_RoadMap.route) {
//            Total_RoadMap()
//        }
    }
}
