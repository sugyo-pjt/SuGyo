package com.ssafy.a602.auth

import com.auth0.android.jwt.JWT
import com.ssafy.a602.auth.api.AuthApiService
import com.ssafy.a602.auth.dto.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 인증 관련 데이터 레이어
 * 로그인, 로그아웃, 토큰 재발행 등의 비즈니스 로직을 처리
 */
@Singleton
class AuthRepository @Inject constructor(
    private val authApiService: AuthApiService,
    private val tokenManager: TokenManager
) {
    
    /**
     * 로그인 처리
     * @param email 사용자 이메일
     * @param password 사용자 비밀번호
     * @return 로그인 결과 (성공/실패)
     */
    suspend fun login(email: String, password: String): AuthResult {
        return try {
            val request = LoginRequest(email, password)
            val response = authApiService.login(request)
            
            if (response.isSuccessful && response.body() != null) {
                val loginData = response.body()!!
                
                // JWT 토큰에서 사용자 ID 추출
                val userId = extractUserIdFromToken(loginData.accessToken) ?: 1L
                
                // 토큰과 사용자 정보 저장
                tokenManager.saveTokensAndUser(
                    accessToken = loginData.accessToken,
                    refreshToken = loginData.refreshToken,
                    userId = userId,
                    email = email
                )
                
                // JWT 토큰에서 사용자 정보 추출
                val userInfo = extractUserInfoFromToken(loginData.accessToken)
                    ?: UserInfo(
                        id = userId.toString(),
                        email = email,
                        nickname = "사용자"
                    )
                
                AuthResult.Success(user = userInfo)
            } else {
                val errorMessage = response.message() ?: "로그인에 실패했습니다."
                AuthResult.Error(errorMessage)
            }
        } catch (e: Exception) {
            AuthResult.Error("네트워크 오류: ${e.message}")
        }
    }
    
    /**
     * 회원가입 처리
     */
    suspend fun signup(
        email: String, 
        nickname: String, 
        password: String, 
        selfIntroduction: String? = null,
        termAgreements: List<TermAgreement> = emptyList()
    ): AuthResult {
        return try {
            val request = SignupRequest(
                email = email,
                password = password,
                nickname = nickname,
                selfIntroduction = selfIntroduction,
                signUpTermAgreements = termAgreements
            )
            val response = authApiService.signup(request)
            
            if (response.isSuccessful) {
                // 회원가입 성공 (서버에서 200 OK만 반환)
                AuthResult.Success(
                    user = UserInfo(
                        id = "1", // 회원가입 후 로그인 필요
                        email = email,
                        nickname = nickname
                    )
                )
            } else {
                val errorMessage = response.message() ?: "회원가입에 실패했습니다."
                AuthResult.Error(errorMessage)
            }
        } catch (e: Exception) {
            AuthResult.Error("네트워크 오류: ${e.message}")
        }
    }
    
    /**
     * 토큰 재발행 처리
     * 서버 스펙에 따라 refreshToken과 userId 모두 필요
     */
    suspend fun reissueToken(): Boolean {
        return try {
            val refreshToken = tokenManager.getRefreshToken()
            val userId = tokenManager.getUserId()
            
            if (refreshToken.isNullOrBlank() || userId == null) {
                return false
            }
            
            val request = ReissueRequest(refreshToken, userId)
            val response = authApiService.reissueToken(request)
            
            if (response.isSuccessful && response.body() != null) {
                val tokenData = response.body()!!
                tokenManager.saveAccessToken(tokenData.accessToken)
                tokenManager.saveRefreshToken(tokenData.refreshToken)
                true
            } else {
                tokenManager.clearTokens()
                false
            }
        } catch (e: Exception) {
            tokenManager.clearTokens()
            false
        }
    }
    
    /**
     * 로그아웃 처리
     */
    suspend fun logout() {
        tokenManager.clearTokens()
    }
    
    /**
     * 로그인 상태 확인
     */
    fun isLoggedIn(): Boolean {
        return tokenManager.isLoggedIn()
    }
    
    /**
     * 저장된 사용자 정보 가져오기
     */
    fun getCurrentUser(): UserInfo? {
        val userId = tokenManager.getUserId()
        val email = tokenManager.getUserEmail()
        val accessToken = tokenManager.getAccessToken()
        
        return if (userId != null && email != null) {
            // JWT 토큰에서 사용자 정보 추출 시도
            if (accessToken != null) {
                extractUserInfoFromToken(accessToken) ?: UserInfo(
                    id = userId.toString(),
                    email = email,
                    nickname = "사용자"
                )
            } else {
                UserInfo(
                    id = userId.toString(),
                    email = email,
                    nickname = "사용자"
                )
            }
        } else {
            null
        }
    }
    
    /**
     * JWT 토큰에서 사용자 ID 추출
     * 서버 JWT 토큰 구조: user_id 필드 사용
     */
    private fun extractUserIdFromToken(accessToken: String): Long? {
        return try {
            val jwt = JWT(accessToken)
            
            // 서버에서 사용하는 필드명: "user_id"
            val userId = jwt.getClaim("user_id").asString()?.toLongOrNull()
            
            userId ?: 1L // 기본값으로 1L 사용
        } catch (e: Exception) {
            // JWT 디코딩 실패시 기본값 반환
            1L
        }
    }
    
    /**
     * JWT 토큰에서 사용자 정보 추출
     * 서버 JWT 토큰 구조: user_id, user_email, user_nickname 필드 사용
     */
    private fun extractUserInfoFromToken(accessToken: String): UserInfo? {
        return try {
            val jwt = JWT(accessToken)
            
            val userId = extractUserIdFromToken(accessToken)
            val email = jwt.getClaim("user_email").asString()
            val nickname = jwt.getClaim("user_nickname").asString()
                ?: "사용자"
            
            UserInfo(
                id = userId?.toString() ?: "1",
                email = email ?: "",
                nickname = nickname
            )
        } catch (e: Exception) {
            null
        }
    }
}
