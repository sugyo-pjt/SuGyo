package com.ssafy.a602.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 인증 가드 ViewModel
 * 토큰 상태 확인 및 자동 로그인 처리
 */
@HiltViewModel
class AuthGuardViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AuthGuardState>(AuthGuardState.Checking)
    val state: StateFlow<AuthGuardState> = _state.asStateFlow()

    init {
        checkAuthState()
    }

    /**
     * 앱 시작시 토큰 상태 확인
     */
    private fun checkAuthState() {
        viewModelScope.launch {
            try {
                val accessToken = tokenManager.getAccessToken()
                val refreshToken = tokenManager.getRefreshToken()

                if (!accessToken.isNullOrBlank()) {
                    // 액세스 토큰이 있으면 인증됨으로 처리
                    _state.value = AuthGuardState.Authenticated(accessToken)
                } else if (!refreshToken.isNullOrBlank()) {
                    // 액세스 토큰은 없지만 리프레시 토큰이 있으면 자동 갱신 시도
                    tryRefreshToken()
                } else {
                    // 토큰이 없으면 인증되지 않음
                    _state.value = AuthGuardState.Unauthenticated
                }
            } catch (e: Exception) {
                // 오류 발생시 인증되지 않음으로 처리
                _state.value = AuthGuardState.Unauthenticated
            }
        }
    }

    /**
     * 리프레시 토큰으로 액세스 토큰 갱신 시도
     */
    private suspend fun tryRefreshToken() {
        try {
            val userId = tokenManager.getUserId()
            if (userId != null) {
                val success = authRepository.reissueToken()
                if (success) {
                    val newAccessToken = tokenManager.getAccessToken()
                    if (!newAccessToken.isNullOrBlank()) {
                        _state.value = AuthGuardState.Authenticated(newAccessToken)
                        return
                    }
                }
            }
        } catch (e: Exception) {
            // 갱신 실패시 토큰 삭제
            tokenManager.clearTokens()
        }
        _state.value = AuthGuardState.Unauthenticated
    }

    /**
     * 로그인 성공시 호출 (LoginScreen에서 토큰을 넘겨받음)
     */
    fun onLoginSucceeded(accessToken: String) {
        _state.value = AuthGuardState.Authenticated(accessToken)
    }
}
