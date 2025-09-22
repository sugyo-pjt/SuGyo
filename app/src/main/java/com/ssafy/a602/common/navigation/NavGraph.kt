package com.ssafy.a602.common.navigation

// ── App Screens ───────────────────────────────────────────────────

// ── Game Screens & Data ───────────────────────────────────────────
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.MutableStateFlow
import com.ssafy.a602.auth.AuthGuard
import com.ssafy.a602.chatbot.ChatbotScreen
import com.ssafy.a602.game.data.GameDataManager
import com.ssafy.a602.game.play.GamePlayScreen
import com.ssafy.a602.game.play.GamePlayViewModel
import com.ssafy.a602.game.preparation.GamePreparationScreen
import com.ssafy.a602.game.ranking.GameRankingScreen
import com.ssafy.a602.game.result.GameResultScreen
import com.ssafy.a602.game.result.GameResultUi
import com.ssafy.a602.game.songs.SongItem
import com.ssafy.a602.game.songs.SongsScreen
import com.ssafy.a602.home.HomeScreen
import com.ssafy.a602.learning.DailyDetailStudyScreen
import com.ssafy.a602.learning.DailyQuizScreen
import com.ssafy.a602.learning.LearningMainPage
import com.ssafy.a602.learning.SongStudyDetailScreen
import com.ssafy.a602.learning.SongStudyListScreen
import com.ssafy.a602.learning.Total_RoadMap
import com.ssafy.a602.login.LoginScreen
import com.ssafy.a602.mypage.MyPageScreen
import com.ssafy.a602.search.SearchScreen
import com.ssafy.a602.search.WordDetailScreen
import com.ssafy.a602.signup.SignUpScreen
import com.ssafy.a602.term.ui.TermDetailScreen
import com.ssafy.a602.term.ui.TermsScreen

@ExperimentalGetImage
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
        startDestination = Screen.AuthGuard.route,
        modifier = modifier
    ) {
        /* ---------- Auth Guard ---------- */
        composable(Screen.AuthGuard.route) {
            AuthGuard(navController = navController)
        }
        
        /* ---------- Login ---------- */
        composable(Screen.Login.route) {
            LoginScreen(
                onBack = {}, // 시작 화면은 뒤로가기 무시 권장
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onForgot = { /* TODO */ },
                onSignup = { navController.navigate(Screen.Signup.route) }
            )
        }

        /* ---------- Signup ---------- */
        composable(Screen.Signup.route) {
            // ✅ 약관 화면에서 돌아올 때 전달되는 동의 결과 구독
            val handle = navController.currentBackStackEntry?.savedStateHandle
            val agreedFlow = handle?.getStateFlow("TERMS_MANDATORY_AGREED", false)
            val externalAgreed by (agreedFlow ?: MutableStateFlow(false))
                .collectAsState(initial = false)

            SignUpScreen(
                onBack = { navController.popBackStack() },
                onPickProfile = { /* TODO */ },
                onOpenTerms = { navController.navigate(Screen.Terms.route) },
                onOpenPrivacy = { /* 사용하지 않음 */ },
                onSignupSuccess = {
                    // 회원가입 완료 → 로그인으로 복귀(스택 정리)
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Signup.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onLogin = { navController.popBackStack() },
                // ✅ 외부 트리거 전달
                externalTermsAgreed = externalAgreed,
                onConsumedExternalTermsAgreed = {
                    handle?.set("TERMS_MANDATORY_AGREED", false)
                }
            )
        }

        /* ---------- Terms ---------- */
        composable(Screen.Terms.route) {
            TermsScreen(navController = navController)
        }

        /* ---------- Term Detail ---------- */
        composable(
            route = Screen.TermDetail.route,
            arguments = listOf(
                navArgument(Screen.TermDetail.ARG_ID) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            TermDetailScreen(
                navController = navController,
                backStackEntry = backStackEntry
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
                onOpenSongStudy = { navController.navigate(Screen.SongStudyList.route) },
//                progressDay = 5 // TODO: 백엔드 값으로 교체
            )
        }

        /* ---------- Roadmap ---------- */
        composable(Screen.Total_RoadMap.route) {
            Total_RoadMap(
                onBack = { navController.popBackStack() },
                onDayClick = { day ->
                    navController.navigate(Screen.DailyStudy.route(day))
                }
            )
        }

        /* ---------- Daily Study (Day별 상세) ---------- */
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
                onStartQuiz = { d -> navController.navigate(Screen.DailyQuiz.route(d)) }
            )
        }

        /* ---------- Daily Quiz (Day별 퀴즈) ---------- */
        composable(
            route = Screen.DailyQuiz.route,
            arguments = listOf(
                navArgument(Screen.DailyQuiz.ARG_DAY) { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val day = backStackEntry.arguments?.getInt(Screen.DailyQuiz.ARG_DAY) ?: 1
            DailyQuizScreen(
                day = day,
                onBack = { navController.popBackStack() },
                onGoStudy = { d -> navController.navigate(Screen.DailyStudy.route(d)) },
                onGoRoadmap = {
                    // 로드맵이 백스택에 없을 수도 있으니 항상 보장되는 방식으로 이동
                    navController.navigate(Screen.Total_RoadMap.route) {
                        popUpTo(Screen.LearningMainPage.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.SongStudyList.route) {
            SongStudyListScreen(
                onBack = { navController.popBackStack() },
                onOpenDetail = { songId ->
                    navController.navigate(Screen.SongStudyDetail.route(songId))
                }
            )
        }

        /* ---------- Song Study : 상세 ---------- */
        composable(
            route = Screen.SongStudyDetail.route,
            arguments = listOf(navArgument(Screen.SongStudyDetail.ARG_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val songId = backStackEntry.arguments?.getString(Screen.SongStudyDetail.ARG_ID)!!
            SongStudyDetailScreen(
                songId = songId,
                onBack = { navController.popBackStack() }
            )
        }

        /* ---------- Search ---------- */
        composable(Screen.Search.route) {
            SearchScreen(
                onOpenDetail = { id ->
                    navController.navigate(Screen.SearchDetail.route(id))
                }
            )
        }

        composable(
            route = Screen.SearchDetail.route,
            arguments = listOf(navArgument(Screen.SearchDetail.ARG_ID) { type = NavType.LongType })
        ) { backStackEntry ->
            val wordId = backStackEntry.arguments?.getLong(Screen.SearchDetail.ARG_ID)!!
            WordDetailScreen(wordId = wordId, onBack = { navController.popBackStack() })
        }

        /* ---------- Chat ---------- */
        composable(Screen.Chat.route) {
            ChatbotScreen(
                onBack = { navController.popBackStack() }
            )
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
                onGameStart = { navController.navigate("game_play/$songId") },
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
            val gamePlayViewModel = remember { GamePlayViewModel() }
            val gameUiState by gamePlayViewModel.ui.collectAsState()
            
            @OptIn(ExperimentalGetImage::class)
            GamePlayScreen(
                songId = songId,
                isPaused = gameUiState.isPaused,
                onTogglePause = { gamePlayViewModel.togglePause() },
                onGameComplete = { _ ->
                    GameDataManager.endGame()
                    navController.navigate("game_result/$songId") {
                        popUpTo("game_play/$songId") { inclusive = true }
                    }
                },
                onGameQuit = {
                    GameDataManager.endGame()
                    navController.navigate(Screen.Game.route) {
                        popUpTo(Screen.Game.route) { inclusive = true }
                    }
                },
                onOpenSettings = { openSettings?.invoke() },
                judgmentResult = null, // TODO: ViewModel 연동 시 교체
                gamePlayViewModel = gamePlayViewModel
            )
        }

        /* ---------- Game : 결과 화면 ---------- */
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
                onRetry = { navController.navigate("game_preparation/$songId") },
                onSubmitRanking = { navController.navigate("game_ranking/$songId") },
                onBackToList = {
                    navController.navigate(Screen.Game.route) {
                        popUpTo(Screen.Game.route) { inclusive = true }
                    }
                }
            )
        }

        /* ---------- Game : 랭킹 ---------- */
        composable("game_ranking/{songId}") { backStackEntry ->
            val songId = backStackEntry.arguments?.getString("songId") ?: ""
            GameRankingScreen(
                songId = songId,
                onBackClick = { navController.popBackStack() }
            )
        }

        /* ---------- MyPage ---------- */
        composable(Screen.MyPage.route) {
            // 필요하면 여기서 onLogout/onWithdraw 로직을 주입
            MyPageScreen(
                onLogout = { /* TODO: 토큰 삭제, 로그인 화면으로 navigate 등 */ },
                onWithdraw = { /* TODO: 회원탈퇴 처리 */ }
            )
        }
    }
}
