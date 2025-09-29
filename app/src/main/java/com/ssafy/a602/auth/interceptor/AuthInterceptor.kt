package com.ssafy.a602.auth.interceptor

import com.ssafy.a602.auth.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 인증 토큰을 자동으로 HTTP 요청 헤더에 추가하는 Interceptor
 * 모든 API 요청에 Authorization 헤더를 자동으로 추가
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // 인증 관련 경로는 토큰 헤더 추가하지 않음
        val isAuthPath = originalRequest.url.encodedPath.startsWith("/api/v1/auth/")
        if (isAuthPath) {
            return chain.proceed(originalRequest)
        }
        
        // 토큰이 있는 경우에만 Authorization 헤더 추가
        val accessToken = tokenManager.getAccessToken()
        
        val newRequest = if (accessToken != null) {
            originalRequest.newBuilder()
                .header(AUTHORIZATION_HEADER, "$BEARER_PREFIX$accessToken")
                .build()
        } else {
            originalRequest
        }
        
        return chain.proceed(newRequest)
    }
}
