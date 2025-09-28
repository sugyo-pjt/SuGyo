package com.ssafy.a602.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a602.auth.TokenManager
import com.ssafy.a602.learning.api.ProgressDetailResponse
import com.ssafy.a602.learning.api.StudyApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

// 화면에서 쓰는 상태/모델들
enum class DayStatus { DONE, CURRENT, LOCKED }

data class DayItem(
    val day: Int,
    val status: DayStatus,
    val correctCount: Int? = null,
    val totalCount: Int? = null
)

sealed interface RoadmapUiState {
    data object Loading : RoadmapUiState
    data class Success(val items: List<DayItem>) : RoadmapUiState
    data class Error(val message: String) : RoadmapUiState
}

@HiltViewModel
class RoadmapViewModel @Inject constructor(
    private val studyApi: StudyApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<RoadmapUiState>(RoadmapUiState.Loading)
    val uiState: StateFlow<RoadmapUiState> = _uiState

    init {
        reload()
    }

    fun reload() {
        // 로그인 안되어 있으면 바로 에러 처리 (원하면 별도 상태로 분리 가능)
        if (!tokenManager.isLoggedIn()) {
            _uiState.value = RoadmapUiState.Error("로그인 필요")
            return
        }

        _uiState.value = RoadmapUiState.Loading
        viewModelScope.launch {
            try {
                val res = studyApi.getProgressDetail()
                if (!res.isSuccessful) {
                    _uiState.value = RoadmapUiState.Error("HTTP ${res.code()} ${res.message()}")
                    return@launch
                }

                val body = res.body() ?: run {
                    _uiState.value = RoadmapUiState.Error("빈 응답")
                    return@launch
                }

                _uiState.value = RoadmapUiState.Success(mapToItems(body))
            } catch (e: IOException) {
                _uiState.value = RoadmapUiState.Error("네트워크 오류: ${e.message ?: "알 수 없음"}")
            } catch (e: HttpException) {
                _uiState.value = RoadmapUiState.Error("HTTP 예외: ${e.code()} ${e.message()}")
            } catch (e: Exception) {
                _uiState.value = RoadmapUiState.Error("오류: ${e.message ?: "알 수 없음"}")
            }
        }
    }

    private fun mapToItems(meta: ProgressDetailResponse): List<DayItem> {
        val total = meta.totalDays.coerceAtLeast(0)
        val progress = meta.progressDay.coerceAtLeast(0)

        return (1..total).map { day ->
            val status = when {
                day <= progress       -> DayStatus.DONE
                day == progress + 1   -> DayStatus.CURRENT
                else                  -> DayStatus.LOCKED
            }
            
            // 해당 Day의 퀴즈 결과 찾기
            val dayResult = meta.days.find { it.day == day }
            
            DayItem(
                day = day,
                status = status,
                correctCount = dayResult?.correctCount,
                totalCount = dayResult?.totalCount
            )
        }
    }
}