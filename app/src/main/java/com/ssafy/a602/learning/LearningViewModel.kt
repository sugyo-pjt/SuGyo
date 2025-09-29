package com.ssafy.a602.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a602.auth.TokenManager
import com.ssafy.a602.learning.api.StudyApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.internal.http2.ConnectionShutdownException
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

data class LearningUiState(
    val isLoading: Boolean = true,
    val progressDay: Int? = null,
    val error: String? = null
)

@HiltViewModel
class LearningViewModel @Inject constructor(
    private val studyApi: StudyApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LearningUiState())
    val uiState: StateFlow<LearningUiState> = _uiState

    init { refresh() }

    fun refresh() {
        // ✅ 로그인 안 되어 있으면 API 호출하지 않음
        if (!tokenManager.isLoggedIn()) {
            _uiState.value = LearningUiState(isLoading = false, error = "로그인 필요")
            return
        }

        _uiState.value = LearningUiState(isLoading = true)
        viewModelScope.launch {
            try {
                val res = studyApi.getProgress()
                if (res.isSuccessful) {
                    _uiState.value = LearningUiState(
                        isLoading = false,
                        progressDay = res.body()?.progressDay ?: 0
                    )
                } else {
                    val code = res.code()
                    val body = res.errorBody()?.string().orEmpty()
                    val msg = when (code) {
                        401 -> "인증 필요(401) – 토큰 만료/누락"
                        403 -> "권한 부족(403) – 접근 권한 확인 필요"
                        else -> "HTTP $code ${res.message()}"
                    } + body.takeIf { it.isNotBlank() }?.let { " | $it" }.orEmpty()

                    _uiState.value = LearningUiState(isLoading = false, error = msg)
                }
            } catch (e: IOException) {
                _uiState.value = LearningUiState(isLoading = false, error = "네트워크 오류: ${e.message ?: "알 수 없음"}")
            } catch (e: ConnectionShutdownException) {
                _uiState.value = LearningUiState(isLoading = false, error = "연결 종료: 다시 시도해주세요")
            } catch (e: HttpException) {
                val reason = e.message ?: ""
                val body   = e.response()?.errorBody()?.string().orEmpty()
                _uiState.value = LearningUiState(
                    isLoading = false,
                    error = "HTTP 예외: ${e.code()} $reason" + body.takeIf { it.isNotBlank() }?.let { " | $it" }.orEmpty()
                )
            } catch (e: Exception) {
                _uiState.value = LearningUiState(isLoading = false, error = "오류: ${e.message ?: "알 수 없음"}")
            }
        }
    }
}
