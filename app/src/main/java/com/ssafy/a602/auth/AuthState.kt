package com.ssafy.a602.auth

/**
 * 인증 가드 상태를 나타내는 sealed interface
 */
sealed interface AuthGuardState {
    /** 토큰 상태 확인 중 */
    data object Checking : AuthGuardState
    
    /** 인증되지 않음 (로그인 필요) */
    data object Unauthenticated : AuthGuardState
    
    /** 인증됨 (토큰 유효) */
    data class Authenticated(val accessToken: String) : AuthGuardState
}
