package com.ssafy.a602.auth.dto

import com.google.gson.annotations.SerializedName

/**
 * 로그인 요청 DTO
 */
data class LoginRequest(
    @SerializedName("email")
    val email: String,
    
    @SerializedName("password")
    val password: String
)

/**
 * 로그인 응답 DTO
 */
data class LoginResponse(
    @SerializedName("accessToken")
    val accessToken: String,
    
    @SerializedName("refreshToken")
    val refreshToken: String
)

/**
 * 사용자 정보
 */
data class UserInfo(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("nickname")
    val nickname: String,
    
    @SerializedName("profileImage")
    val profileImage: String? = null
)

/**
 * 회원가입 요청 DTO
 */
data class SignupRequest(
    @SerializedName("email")
    val email: String,
    
    @SerializedName("password")
    val password: String,
    
    @SerializedName("nickname")
    val nickname: String,
    
    @SerializedName("selfIntroduction")
    val selfIntroduction: String? = null,
    
    @SerializedName("signUpTermAgreements")
    val signUpTermAgreements: List<TermAgreement>
)

/**
 * 약관 동의 DTO
 */
data class TermAgreement(
    @SerializedName("termId")
    val termId: Int,
    
    @SerializedName("agreed")
    val agreed: Boolean
)

/**
 * 회원가입 응답 DTO
 * 서버에서 200 OK만 반환하므로 단순화
 */
data class SignupResponse(
    @SerializedName("success")
    val success: Boolean = true,
    
    @SerializedName("message")
    val message: String = "회원가입 성공"
)

/**
 * 토큰 재발행 요청 DTO
 * 서버 스펙에 따라 refreshToken과 userId 모두 필요
 */
data class ReissueRequest(
    @SerializedName("refreshToken")
    val refreshToken: String,
    
    @SerializedName("userId")
    val userId: Long
)

/**
 * 토큰 재발행 응답 DTO
 */
data class ReissueResponse(
    @SerializedName("accessToken")
    val accessToken: String,
    
    @SerializedName("refreshToken")
    val refreshToken: String
)
