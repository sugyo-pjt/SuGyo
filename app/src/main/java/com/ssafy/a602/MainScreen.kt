package com.ssafy.a602

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ssafy.a602.common.navigation.NavGraph
import com.ssafy.a602.navbar.BottomTab
import com.ssafy.a602.navbar.CustomBottomNavigationBar


@Composable
fun MainScreen(
    permissionLauncher: ((Array<String>) -> Unit)? = null,
    snackbarHostState: SnackbarHostState? = null,
    openSettings: (() -> Unit)? = null
) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // 현재 라우트에 따라 선택된 탭 결정 (Screen.kt의 route와 일치)
    val selectedTab = when (currentRoute) {
        "search" -> BottomTab.SEARCH
        "learning" -> BottomTab.LEARNING
        "chat" -> BottomTab.CHAT
        "game" -> BottomTab.GAME
        "mypage" -> BottomTab.MYPAGE
        else -> BottomTab.SEARCH  // 기본값
    }

    // 바텀바 표시 규칙
    // - 인증가드/로그인/홈 화면에서는 네비게이션 바 숨김
    // - 리듬게임 준비/플레이/결과 화면에서도 숨김 (게임 중 UI 몰입을 위해)
    val showBottomBar = when (currentRoute) {
        "auth_guard" -> false
        "login" -> false
        "home" -> false
        "signup" -> false
        null -> false
        else -> !(currentRoute?.startsWith("game_preparation") == true ||
                currentRoute?.startsWith("game_play") == true ||
                currentRoute?.startsWith("game_result") == true ||
                currentRoute?.startsWith("game_ranking") == true)
    }

    // 탭 선택 시 해당 화면으로 이동 (Screen.kt의 route와 일치)
    val onTabSelected: (BottomTab) -> Unit = { tab ->
        val route = when (tab) {
            BottomTab.SEARCH -> "search"
            BottomTab.LEARNING -> "learning"
            BottomTab.CHAT -> "chat"
            BottomTab.GAME -> "game"
            BottomTab.MYPAGE -> "mypage"
        }

        // 현재 라우트와 같으면 네비게이션하지 않음
        if (currentRoute != route) {
            navController.navigate(route) {
                // 백스택에 중복된 화면이 쌓이지 않도록 설정
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                CustomBottomNavigationBar(
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected
                )
            }
        },
        // 스낵바가 필요한 화면에서 사용 가능 (null이면 미표시)
        snackbarHost = {
            snackbarHostState?.let { SnackbarHost(it) }
        }
    ) { innerPadding ->
        // NavGraph: 실제 라우팅 처리 (권한 요청/설정 이동 콜백을 그대로 전달)
        NavGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            permissionLauncher = permissionLauncher,
            snackbarHostState = snackbarHostState,
            openSettings = openSettings
        )
    }
}
