package com.ssafy.a602.auth.api

import com.ssafy.a602.auth.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 인증 관련 API 서비스
 * 로그인, 회원가입, 토큰 재발행 등의 API 엔드포인트 정의
 */
interface AuthApiService {
    
    /**
     * 로그인 API
     * @param request 로그인 요청 데이터 (이메일, 비밀번호)
     * @return 로그인 응답 (액세스 토큰, 리프레시 토큰)
     */
    @POST("/api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
    
    /**
     * 회원가입 API
     * @param request 회원가입 요청 데이터 (이메일, 닉네임, 비밀번호, 자기소개, 약관동의)
     * @return 회원가입 응답 (200 OK)
     */
    @POST("/api/v1/user/signup")
    suspend fun signup(@Body request: SignupRequest): Response<SignupResponse>
    
    /**
     * 토큰 재발행 API
     * @param request 리프레시 토큰과 사용자 ID
     * @return 새로운 액세스 토큰과 리프레시 토큰
     */
    @POST("/api/v1/auth/reissue-token")
    suspend fun reissueToken(@Body request: ReissueRequest): Response<ReissueResponse>
}
