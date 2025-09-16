package com.ssafy.a602.auth.interceptor

import com.ssafy.a602.auth.TokenManager
import com.ssafy.a602.auth.api.AuthApiService
import com.ssafy.a602.auth.dto.ReissueRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * 401 응답시 자동으로 토큰을 재발행하는 Authenticator
 * 동시성 제어를 통해 중복 재발행을 방지하고, 무한루프를 방지합니다.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    @Named("TokenRefresh") private val tokenRefreshApiService: AuthApiService
) : Authenticator {

    @Volatile
    private var isRefreshing = false
    private val lock = Object()

    override fun authenticate(route: Route?, response: Response): Request? {
        // 이미 재시도된 요청은 중복 방지
        if (response.request.header("X-Retry") == "1") {
            return null
        }

        synchronized(lock) {
            if (!isRefreshing) {
                isRefreshing = true
                try {
                    val refreshToken = tokenManager.getRefreshToken()
                    val userId = tokenManager.getUserId()
                    
                    if (refreshToken.isNullOrBlank() || userId == null) {
                        tokenManager.clearTokens()
                        return null
                    }

                            val newTokens = runCatching {
                                runBlocking {
                                    tokenRefreshApiService.reissueToken(
                                        ReissueRequest(refreshToken, userId)
                                    )
                                }
                            }.getOrElse {
                        tokenManager.clearTokens()
                        return null
                    }

                    if (newTokens.isSuccessful && newTokens.body() != null) {
                        val tokenData = newTokens.body()!!
                        tokenManager.saveAccessToken(tokenData.accessToken)
                        tokenManager.saveRefreshToken(tokenData.refreshToken)
                    } else {
                        tokenManager.clearTokens()
                        return null
                    }
                } finally {
                    isRefreshing = false
                    lock.notifyAll()
                }
            } else {
                // 다른 스레드가 갱신 중이면 완료될 때까지 대기
                try {
                    lock.wait(500L)
                } catch (_: InterruptedException) {
                    return null
                }
            }
        }

        val newAccessToken = tokenManager.getAccessToken()
        return if (!newAccessToken.isNullOrBlank()) {
            response.request.newBuilder()
                .header("Authorization", "Bearer $newAccessToken")
                .header("X-Retry", "1")
                .build()
        } else {
            null
        }
    }
}
