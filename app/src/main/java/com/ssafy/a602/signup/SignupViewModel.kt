package com.ssafy.a602.signup

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
 * 회원가입 화면 ViewModel
 * 회원가입 관련 UI 상태와 비즈니스 로직 관리
 */
@HiltViewModel
class SignupViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {
    
    // UI 상태 관리
    private val _uiState = MutableStateFlow(SignupUiState())
    val uiState: StateFlow<SignupUiState> = _uiState.asStateFlow()
    
    /**
     * 회원가입 처리
     */
    fun signup(email: String, nickname: String, password: String, selfIntroduction: String? = null) {
        if (email.isBlank() || nickname.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "모든 필드를 입력해주세요."
            )
            return
        }
        
        if (!isValidEmail(email)) {
            _uiState.value = _uiState.value.copy(
                error = "올바른 이메일 형식을 입력해주세요."
            )
            return
        }
        
        if (nickname.trim().length < 2) {
            _uiState.value = _uiState.value.copy(
                error = "닉네임은 2자 이상 입력해주세요."
            )
            return
        }
        
        if (password.length < 8) {
            _uiState.value = _uiState.value.copy(
                error = "비밀번호는 8자 이상 입력해주세요."
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            when (val result = authManager.signup(email, nickname, password, selfIntroduction)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        signupSuccess = true
                    )
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
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
    }
    
    /**
     * 회원가입 성공 상태 초기화
     */
    fun clearSignupSuccess() {
        _uiState.value = _uiState.value.copy(signupSuccess = false)
    }
    
    /**
     * 이메일 형식 검증
     */
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}

/**
 * 회원가입 화면 UI 상태
 */
data class SignupUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val signupSuccess: Boolean = false
)
