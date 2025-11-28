package com.ssafy.a602.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.ssafy.a602.login.LoginScreen
import com.ssafy.a602.common.navigation.Screen
import androidx.navigation.NavController

/**
 * 인증 가드 Composable
 * 앱 시작시 로그인 상태를 확인하고 적절한 화면으로 분기
 */
@Composable
fun AuthGuard(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: AuthGuardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // ✅ 네비게이션은 사이드이펙트에서 1회만 수행
    LaunchedEffect(state) {
        if (state is AuthGuardState.Authenticated) {
            navController.navigate(Screen.Home.route) {
                // ⚠️ popUpTo 대상 route가 그래프 안에 반드시 존재해야 합니다.
                popUpTo(Screen.AuthGuard.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    when (state) {
        AuthGuardState.Checking -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        AuthGuardState.Unauthenticated -> {
            LoginScreen(
                onBack = {}, // 시작화면이면 뒤로가기 무시
                onLoginSuccess = { accessToken ->
                    // ❗ 여기선 네비게이션 금지(상태만 변경)
                    viewModel.onLoginSucceeded(accessToken)
                },
                onForgot = { /* TODO: 비밀번호 재설정 구현 */ },
                onSignup = { navController.navigate(Screen.Signup.route) }
            )
        }

        is AuthGuardState.Authenticated -> {
            // 네비게이션 전까지 잠깐 스플래시로 버티기 (화면 깜빡임 방지)
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
