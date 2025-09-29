package com.ssafy.a602.auth.interceptor

import android.util.Log
import com.ssafy.a602.BuildConfig
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
        // 🔎 디버그 로그: 어떤 응답으로 호출됐는지, Authorization 헤더가 있었는지
        if (BuildConfig.DEBUG) {
            val auth = response.request.header("Authorization")
            val masked = auth
                ?.removePrefix("Bearer ")
                ?.take(10)
                ?.plus("...") ?: "none"
            Log.w(
                TAG,
                "authenticate() called: code=${response.code}, " +
                        "priorResponses=${responseCount(response)}, " +
                        "url=${response.request.url}, auth=$masked"
            )
        }

        // 이미 재시도된 요청은 중복 방지
        if (response.request.header("X-Retry") == "1") {
            if (BuildConfig.DEBUG) Log.d(TAG, "Skip: already retried once.")
            return null
        }

        synchronized(lock) {
            if (!isRefreshing) {
                isRefreshing = true
                try {
                    val refreshToken = tokenManager.getRefreshToken()
                    val userId = tokenManager.getUserId()

                    if (refreshToken.isNullOrBlank() || userId == null) {
                        if (BuildConfig.DEBUG) Log.e(TAG, "No refreshToken/userId. Clear & abort.")
                        tokenManager.clearTokens()
                        return null
                    }

                    if (BuildConfig.DEBUG) Log.d(TAG, "Reissuing token… (userId=$userId)")
                    val newTokens = runCatching {
                        runBlocking {
                            tokenRefreshApiService.reissueToken(
                                ReissueRequest(refreshToken, userId)
                            )
                        }
                    }.getOrElse { e ->
                        if (BuildConfig.DEBUG) Log.e(TAG, "Reissue call failed: ${e.message}")
                        tokenManager.clearTokens()
                        return null
                    }

                    if (newTokens.isSuccessful && newTokens.body() != null) {
                        val tokenData = newTokens.body()!!
                        tokenManager.saveAccessToken(tokenData.accessToken)
                        tokenManager.saveRefreshToken(tokenData.refreshToken)
                        if (BuildConfig.DEBUG) Log.i(TAG, "Reissue success. New token saved.")
                    } else {
                        if (BuildConfig.DEBUG) {
                            Log.e(
                                TAG,
                                "Reissue HTTP ${newTokens.code()} " +
                                        newTokens.errorBody()?.string().orEmpty()
                            )
                        }
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
                    if (BuildConfig.DEBUG) Log.d(TAG, "Waiting for other refresh…")
                    lock.wait(500L)
                } catch (_: InterruptedException) {
                    return null
                }
            }
        }

        val newAccessToken = tokenManager.getAccessToken()
        return if (!newAccessToken.isNullOrBlank()) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Retrying request with new token.")
            response.request.newBuilder()
                .header("Authorization", "Bearer $newAccessToken")
                .header("X-Retry", "1")
                .build()
        } else {
            if (BuildConfig.DEBUG) Log.e(TAG, "No new access token after refresh.")
            null
        }
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }

    companion object {
        private const val TAG = "TokenAuthenticator"
    }
}
