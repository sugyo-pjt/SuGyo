package com.ssafy.a602.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a602.auth.AuthManager
import com.ssafy.a602.auth.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 로그인 화면 ViewModel
 * 로그인 관련 UI 상태와 비즈니스 로직 관리
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {
    
    // UI 상태 관리
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    // AuthManager의 상태를 관찰
    init {
        viewModelScope.launch {
            authManager.authState.collect { authState ->
                _uiState.value = _uiState.value.copy(
                    isLoading = authState.isLoading,
                    error = authState.error,
                    isLoggedIn = authState.isLoggedIn
                )
            }
        }
    }
    
    /**
     * 로그인 처리
     */
    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "이메일과 비밀번호를 입력해주세요."
            )
            return
        }
        
        if (!isValidEmail(email)) {
            _uiState.value = _uiState.value.copy(
                error = "올바른 이메일 형식을 입력해주세요."
            )
            return
        }
        
        viewModelScope.launch {
            when (val result = authManager.login(email, password)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        error = null,
                        loginSuccess = true
                    )
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message
                    )
                }
            }
        }
    }
    
    /**
     * 에러 상태 초기화
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
        authManager.clearError()
    }
    
    /**
     * 로그인 성공 상태 초기화
     */
    fun clearLoginSuccess() {
        _uiState.value = _uiState.value.copy(loginSuccess = false)
    }
    
    /**
     * 이메일 형식 검증
     */
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}

/**
 * 로그인 화면 UI 상태
 */
data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val loginSuccess: Boolean = false
)
