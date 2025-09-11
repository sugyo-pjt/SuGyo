package com.ssafy.a602.common.navigation  // 패키지 경로(모듈/폴더 구조 상의 네임스페이스)

// ── AndroidX / Compose ─────────────────────────────────────────────
import androidx.camera.core.ExperimentalMirrorMode
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
import androidx.navigation.NavType
import androidx.navigation.navArgument

// ── App Screens ───────────────────────────────────────────────────
import com.ssafy.a602.home.HomeScreen
import com.ssafy.a602.learning.LearningMainPage
import com.ssafy.a602.learning.Total_RoadMap
import com.ssafy.a602.login.LoginScreen
import com.ssafy.a602.learning.DailyDetailStudyScreen

// ── Game Screens & Data ───────────────────────────────────────────
import com.ssafy.a602.game.songs.SongsScreen
import com.ssafy.a602.game.preparation.GamePreparationScreen
import com.ssafy.a602.game.play.GamePlayScreen
import com.ssafy.a602.game.result.GameResultScreen
import com.ssafy.a602.game.ranking.GameRankingScreen
import com.ssafy.a602.game.result.GameResultUi
import com.ssafy.a602.game.songs.SongItem
import com.ssafy.a602.game.data.GameDataManager

@OptIn(ExperimentalMirrorMode::class)
@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    permissionLauncher: ((Array<String>) -> Unit)? = null,
    snackbarHostState: SnackbarHostState? = null,
    openSettings: (() -> Unit)? = null
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
        modifier = modifier
    ) {
        /* ---------- Login ---------- */
        composable(Screen.Login.route) {
            // 시작 화면: 뒤로가기는 보통 무시
            LoginScreen(
                onBack = {},
                onSubmit = { email, password ->
                    // TODO: 인증 성공 시 홈으로 이동
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onForgot = { /* navController.navigate("forgot") */ },
                onSignup = { /* navController.navigate("signup") */ }
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
                onDayClick = { day ->
                    // Day별 상세 학습으로 이동 (Screen.DailyStudy가 존재해야 함)
                    navController.navigate(Screen.DailyStudy.route(day))
                }
            )
        }

        /* ---------- Daily Study (Day별 상세 학습) ---------- */
        composable(
            route = Screen.DailyStudy.route,
            arguments = listOf(
                navArgument(Screen.DailyStudy.ARG_DAY) { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val day = backStackEntry.arguments?.getInt(Screen.DailyStudy.ARG_DAY) ?: 1
            DailyDetailStudyScreen(
                day = day,
                onBack = { navController.popBackStack() },
                onStartQuiz = { /* navController.navigate("quiz/$it") */ }
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

        /* ---------- Game : 곡 선택 ---------- */
        composable(Screen.Game.route) {
            SongsScreen(
                onSongClick = { song ->
                    navController.navigate("game_preparation/${song.id}")
                },
                permissionLauncher = permissionLauncher,
                openSettings = openSettings
            )
        }

        /* ---------- Game : 준비 화면 ---------- */
        composable("game_preparation/{songId}") { backStackEntry ->
            val songId = backStackEntry.arguments?.getString("songId") ?: ""

            val song = GameDataManager.currentSong.value?.takeIf {
                GameDataManager.isCurrentSong(songId)
            } ?: SongItem(
                id = songId,
                title = "알 수 없는 곡",
                artist = "알 수 없는 아티스트",
                durationText = "0:00",
                bestScore = null
            )

            GamePreparationScreen(
                song = song,
                onGameStart = {
                    navController.navigate("game_play/${songId}")
                },
                onBack = {
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
                songId = songId,
                isPaused = false,
                onTogglePause = { /* TODO: 일시정지 토글 로직(ViewModel 연동) */ },
                onGameComplete = { gameResult ->
                    GameDataManager.endGame()
                    navController.navigate("game_result/${songId}") {
                        popUpTo("game_play/${songId}") { inclusive = true }
                    }
                },
                onGameQuit = {
                    GameDataManager.endGame()
                    navController.navigate(Screen.Game.route) {
                        popUpTo(Screen.Game.route) { inclusive = true }
                    }
                },
                onOpenSettings = { openSettings?.invoke() },
                judgmentResult = null // 실제 게임에서는 ViewModel에서 관리
            )
        }

        /* ---------- Game Result ---------- */
        composable("game_result/{songId}") { backStackEntry ->
            val songId = backStackEntry.arguments?.getString("songId") ?: ""
            val gameResult = GameDataManager.lastGameResult.value ?: GameResultUi(
                songTitle = "알 수 없는 곡",
                score = 0,
                accuracyPercent = 0,
                grade = "F",
                maxCombo = 0,
                correctCount = 0,
                missCount = 0,
                comboMultiplier = 1.0,
                isNewRecord = false,
                missWords = emptyList()
            )

            GameResultScreen(
                result = gameResult,
                onBack = {
                    navController.navigate(Screen.Game.route) {
                        popUpTo(Screen.Game.route) { inclusive = true }
                    }
                },
                onRetry = {
                    navController.navigate("game_preparation/${songId}")
                },
                onSubmitRanking = {
                    navController.navigate("game_ranking/${songId}")
                },
                onBackToList = {
                    navController.navigate(Screen.Game.route) {
                        popUpTo(Screen.Game.route) { inclusive = true }
                    }
                }
            )
        }

        /* ---------- Game Ranking ---------- */
        composable("game_ranking/{songId}") { backStackEntry ->
            val songId = backStackEntry.arguments?.getString("songId") ?: ""
            GameRankingScreen(
                songId = songId,
                onBackClick = { navController.popBackStack() }
            )
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
