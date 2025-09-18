package com.ssafy.a602.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a602.game.api.RetrofitClient
import com.ssafy.a602.auth.interceptor.AuthInterceptor
import com.ssafy.a602.auth.interceptor.TokenAuthenticator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.internal.http2.ConnectionShutdownException
import retrofit2.Response
import retrofit2.http.GET
import java.io.IOException
import javax.inject.Inject

// --- API & DTO ----------------------------------------------------
data class ProgressResponse(val progressDay: Int)

interface StudyApiService {
    @GET("/api/v1/study/progress")
    suspend fun getProgress(): Response<ProgressResponse>
}

// --- UI State -----------------------------------------------------
data class LearningUiState(
    val isLoading: Boolean = true,
    val progressDay: Int? = null,
    val error: String? = null
)

// --- ViewModel ----------------------------------------------------
@HiltViewModel
class LearningViewModel @Inject constructor(
    private val authInterceptor: AuthInterceptor,
    private val tokenAuthenticator: TokenAuthenticator
) : ViewModel() {

    companion object { private const val TAG = "LearningVM" }

    // RetrofitClient(BASE_URL) + 인증 인터셉터/인증기 그대로 사용
    private val api: StudyApiService by lazy {
        val ok = RetrofitClient.createOkHttpClient(authInterceptor, tokenAuthenticator)
        RetrofitClient.createRetrofit(ok).create(StudyApiService::class.java)
    }

    private val _uiState = MutableStateFlow(LearningUiState())
    val uiState: StateFlow<LearningUiState> = _uiState

    init { refresh() }

    fun refresh() {
        _uiState.value = LearningUiState(isLoading = true)
        viewModelScope.launch {
            try {
                val res = api.getProgress()
                android.util.Log.d(TAG, "GET /study/progress -> ${res.code()}")

                if (res.isSuccessful) {
                    _uiState.value = LearningUiState(
                        isLoading = false,
                        progressDay = res.body()?.progressDay ?: 0
                    )
                } else {
                    val code = res.code()
                    val body = res.errorBody()?.string().orEmpty()
                    val message = when (code) {
                        401 -> "인증 필요(401) – 토큰 만료/누락"
                        403 -> "권한 부족(403) – 권한(role) 또는 owner 아님"
                        else -> "HTTP $code ${res.message()}"
                    } + body.takeIf { it.isNotBlank() }?.let { " | $it" }.orEmpty()

                    _uiState.value = LearningUiState(
                        isLoading = false,
                        error = message
                    )
                }
            } catch (e: IOException) {
                // 네트워크 끊김/타임아웃 등
                android.util.Log.e(TAG, "network error", e)
                _uiState.value = LearningUiState(
                    isLoading = false,
                    error = "네트워크 오류: ${e.message ?: "알 수 없음"}"
                )
            } catch (e: ConnectionShutdownException) {
                android.util.Log.e(TAG, "connection shutdown", e)
                _uiState.value = LearningUiState(
                    isLoading = false,
                    error = "연결 종료: 다시 시도해주세요"
                )
            } catch (e: Exception) {
                android.util.Log.e(TAG, "unexpected", e)
                _uiState.value = LearningUiState(
                    isLoading = false,
                    error = "오류: ${e.message ?: "알 수 없음"}"
                )
            }
        }
    }
}
