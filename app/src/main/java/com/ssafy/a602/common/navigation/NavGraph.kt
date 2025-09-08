package com.ssafy.a602.common.navigation  // 패키지 경로(모듈/폴더 구조 상의 네임스페이스)

// ── 필요한 것만 한 번씩만 import ─────────────────────────────────────────────
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

// ── 앱 화면 컴포넌트들 ────────────────────────────────────────────────────────
import com.ssafy.a602.home.HomeScreen
import com.ssafy.a602.learning.LearningMainPage
import com.ssafy.a602.learning.Total_RoadMap
import com.ssafy.a602.login.LoginScreen   // ✅ import했으니 아래에서 그냥 LoginScreen(...) 으로 호출

// ── 게임 관련 화면 및 데이터 ──────────────────────────────────────────────────
import com.ssafy.a602.game.SongsScreen
import com.ssafy.a602.game.GamePreparationScreen
import com.ssafy.a602.game.GamePlayScreen
import com.ssafy.a602.game.Song
import com.ssafy.a602.game.data.GameDataManager

// ──────────────────────────────────────────────────────────────────────────────
/**
 * 앱의 네비게이션 그래프를 정의.
 *
 * - 상위에서 rememberNavController()로 생성한 NavHostController를 주입받아, 각 route와 Composable 화면을 연결한다.
 * - startDestination은 "Login"으로 설정(로그인 성공 시 Home으로 이동하면서 Login을 백스택에서 제거).
 * - Game 라우트는 곡 선택 → 준비 → 플레이의 3단계 흐름으로 구성.
 *
 * @param navController 화면 전환을 수행하는 컨트롤러(상위에서 rememberNavController()로 생성)
 * @param modifier 상위에서 전달받은 Modifier(여기선 통째로 NavHost에 전달)
 * @param permissionLauncher 권한 요청 런처 (카메라/오디오 등 필요 시 사용). null이면 내부에서 사용 안 함.
 * @param snackbarHostState 스낵바 표시용 호스트 (필요 시 화면에서 사용)
 * @param openSettings 앱 설정 화면(권한 화면 등)으로 이동시키는 콜백
 */
@Composable
fun NavGraph(
    navController: NavHostController, // 화면 전환 컨트롤러
    modifier: Modifier = Modifier,     // NavHost 자체에 Modifier 적용
    permissionLauncher: ((Array<String>) -> Unit)? = null,
    snackbarHostState: SnackbarHostState? = null,
    openSettings: (() -> Unit)? = null
) {
    NavHost(
        navController = navController,            // 어떤 컨트롤러로 그래프를 운용할지 지정
        startDestination = Screen.Login.route,    // ✅ 시작 화면: 로그인
        modifier = modifier
    ) {
        /* ---------- Login ---------- */
        composable(Screen.Login.route) {
            // 시작화면이라 뒤로가기는 무의미 → onBack = {} 그대로 두면 됨
            LoginScreen(
                onBack = {}, // 로그인 첫화면이면 뒤로가기 동작 없음 권장
                onSubmit = { email, password ->
                    // TODO: 실제 인증 로직(성공 시 아래 네비게이션 실행)
                    navController.navigate(Screen.Home.route) {
                        // ✅ 로그인 화면을 백스택에서 제거 → 뒤로가기로 로그인 복귀 X
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onForgot = { /* navController.navigate("forgot") 등 필요 시 구현 */ },
                onSignup = { /* navController.navigate("signup") 등 필요 시 구현 */ }
            )
        }

        /* ---------- Home ---------- */
        composable(Screen.Home.route) {
            HomeScreen(
                onGoLearning = { navController.navigate(Screen.LearningMainPage.route) }, // ← 버튼 클릭 시 이동
                onOpenChat   = { navController.navigate(Screen.Chat.route) },
                onOpenGame   = { navController.navigate(Screen.Game.route) },
                onOpenMyPage = { navController.navigate(Screen.MyPage.route) }
            )  // 홈 라우트 진입 시 HomeScreen 컴포저블 표시
        }

        /* ---------- Learning Main ---------- */
        composable(Screen.LearningMainPage.route) {
            // 학습 메인(로드맵 진입 버튼/진도율 등)
            LearningMainPage(
                onStartRoadmap = { navController.navigate(Screen.Total_RoadMap.route) },
                progressDay = 5 // TODO: 백엔드 값으로 교체
            )
        }

        /* ---------- Roadmap ---------- */
        composable(Screen.Total_RoadMap.route) {
            // 듀오링고 스타일의 Day1, Day2... 구성
            Total_RoadMap(
                onBack = { navController.popBackStack() },
                onDayClick = { /* day -> navController.navigate("lesson/$day") 등 필요 시 구현 */ }
            )
        }

        /* ---------- Search ---------- */
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

        /* ---------- Chat ---------- */
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

        /* ---------- Game : 곡 선택 ---------- */
        composable(Screen.Game.route) {
            // 곡 리스트 화면(SongsScreen) → 곡 클릭 시 준비 화면으로 전환
            SongsScreen(
                onSongClick = { song ->
                    navController.navigate("game_preparation/${song.id}")
                },
                permissionLauncher = permissionLauncher, // 권한 필요 시 사용(카메라/오디오 등)
                openSettings = openSettings              // 권한 거부 시 설정으로 이동
            )
        }

        /* ---------- Game : 준비 화면 ---------- */
        composable("game_preparation/{songId}") { backStackEntry ->
            val songId = backStackEntry.arguments?.getString("songId") ?: ""

            // GameDataManager에서 현재 선택된 곡 가져오기(없으면 placeholder 구성)
            val song = GameDataManager.currentSong.value?.takeIf {
                GameDataManager.isCurrentSong(songId)
            } ?: Song(
                id = songId,
                title = "알 수 없는 곡",
                artist = "알 수 없는 아티스트",
                durationText = "0:00",
                bpm = 120,
                rating = 0.0,
                bestScore = null
            )

            GamePreparationScreen(
                song = song,
                onGameStart = {
                    // 준비 완료 → 실제 게임 화면으로 진입
                    navController.navigate("game_play/${songId}")
                },
                onBack = {
                    // SongsScreen으로 돌아가기(백스택 정리)
                    navController.navigate(Screen.Game.route) {
                        popUpTo(Screen.Game.route) { inclusive = true }
                    }
                },
                permissionLauncher = permissionLauncher,
                openSettings = openSettings
            )
        }

        /* ---------- Game : 플레이 화면 ---------- */
        composable("game_play/{songId}") { backStackEntry ->
            val songId = backStackEntry.arguments?.getString("songId") ?: ""

            GamePlayScreen(
                isPaused = false,
                onTogglePause = { /* TODO: 일시정지 토글 로직 연결(ViewModel 등) */ },
                onEnd = {
                    // 게임 종료 처리
                    GameDataManager.endGame()
                    // 곡 리스트(SongsScreen)로 복귀(백스택 정리)
                    navController.navigate(Screen.Game.route) {
                        popUpTo(Screen.Game.route) { inclusive = true }
                    }
                },
                onOpenSettings = { openSettings?.invoke() },
                judgmentResult = null // 실제 게임에서는 ViewModel에서 관리
            )
        }

        /* ---------- MyPage ---------- */
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
