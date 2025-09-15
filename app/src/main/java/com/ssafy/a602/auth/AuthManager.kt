package com.ssafy.a602.auth

import com.ssafy.a602.auth.dto.TermAgreement
import com.ssafy.a602.auth.dto.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 인증 상태 관리 클래스
 * UI 레이어에서 사용하는 인증 상태를 관리하고 Repository와 연결
 */
@Singleton
class AuthManager @Inject constructor(
    private val authRepository: AuthRepository
) {
    
    // 인증 상태를 관리하는 StateFlow
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    init {
        // 앱 시작시 저장된 토큰으로 로그인 상태 확인
        checkAuthState()
    }
    
    /**
     * 로그인 처리
     */
    suspend fun login(email: String, password: String): AuthResult {
        _authState.value = _authState.value.copy(isLoading = true, error = null)
        
        val result = authRepository.login(email, password)
        
        when (result) {
            is AuthResult.Success -> {
                _authState.value = AuthState(
                    isLoggedIn = true,
                    isLoading = false,
                    user = result.user
                )
            }
            is AuthResult.Error -> {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = result.message
                )
            }
        }
        
        return result
    }
    
    /**
     * 회원가입 처리
     */
    suspend fun signup(
        email: String, 
        nickname: String, 
        password: String, 
        selfIntroduction: String? = null,
        termAgreements: List<TermAgreement> = listOf(
            TermAgreement(1, true), // 필수 약관
            TermAgreement(2, true)  // 필수 약관
        )
    ): AuthResult {
        _authState.value = _authState.value.copy(isLoading = true, error = null)
        
        val result = authRepository.signup(email, nickname, password, selfIntroduction, termAgreements)
        
        _authState.value = _authState.value.copy(isLoading = false)
        
        return result
    }
    
    /**
     * 토큰 재발행 처리
     */
    suspend fun reissueToken(): Boolean {
        return authRepository.reissueToken()
    }
    
    /**
     * 로그아웃 처리
     */
    suspend fun logout() {
        authRepository.logout()
        _authState.value = AuthState()
    }
    
    /**
     * 저장된 토큰으로 인증 상태 확인
     */
    private fun checkAuthState() {
        if (authRepository.isLoggedIn()) {
            val user = authRepository.getCurrentUser()
            if (user != null) {
                _authState.value = AuthState(
                    isLoggedIn = true,
                    user = user
                )
            }
        }
    }
    
    /**
     * 에러 상태 초기화
     */
    fun clearError() {
        _authState.value = _authState.value.copy(error = null)
    }
}

/**
 * 인증 상태 데이터 클래스
 */
data class AuthState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val user: UserInfo? = null,
    val error: String? = null
)

/**
 * 인증 결과를 나타내는 sealed class
 */
sealed class AuthResult {
    data class Success(val user: UserInfo) : AuthResult()
    data class Error(val message: String) : AuthResult()
}
