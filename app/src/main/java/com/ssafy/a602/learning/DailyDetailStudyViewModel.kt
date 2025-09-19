package com.ssafy.a602.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a602.learning.api.DayItemsResponse
import com.ssafy.a602.learning.api.StudyApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/* ──────────────────────────────────────────────────────────────────────────
 * 화면에 뿌리기 쉬운 UI 전용 모델
 *  - 서버 DTO를 그대로 쓰면 필요 없는 속성/nullable이 섞여서 복잡해짐
 *  - 화면에서 쓰는 최소 속성만 뽑아 온 모델로 매핑해서 전달
 * ────────────────────────────────────────────────────────────────────────── */
data class DailyStudyItem(
    val word: String,
    val videoUrl: String? = null
)

/* ──────────────────────────────────────────────────────────────────────────
 * 화면 상태 (로딩/성공/에러)
 * ────────────────────────────────────────────────────────────────────────── */
sealed interface DailyDetailUiState {
    data object Loading : DailyDetailUiState
    data class Success(val items: List<DailyStudyItem>) : DailyDetailUiState
    data class Error(val message: String) : DailyDetailUiState
}

/* ──────────────────────────────────────────────────────────────────────────
 * ViewModel
 *  - Hilt로 StudyApiService 주입
 *  - load(day) 호출 시 네트워크 통신 → 상태 스트림(_uiState)에 반영
 *  - 401 발생 시 TokenAuthenticator가 동작하여 토큰 재발급/재시도를 처리
 * ────────────────────────────────────────────────────────────────────────── */
@HiltViewModel
class DailyDetailStudyViewModel @Inject constructor(
    private val studyApi: StudyApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<DailyDetailUiState>(DailyDetailUiState.Loading)
    val uiState: StateFlow<DailyDetailUiState> = _uiState

    /** Day 상세 데이터 로드 트리거 */
    fun load(day: Int) {
        _uiState.value = DailyDetailUiState.Loading

        viewModelScope.launch {
            try {
                // 네트워크 호출
                val res = studyApi.getDayDetail(day)

                if (res.isSuccessful && res.body() != null) {
                    // 성공 → DTO를 화면용 모델로 변환
                    val body: DayItemsResponse = res.body()!!
                    val delim = Regex("[,，、/|]") // 콤마·중국어 콤마·일본어 ‘、’·슬래시·파이프
                    val items = body.items.map { dto ->
                        val firstWord = dto.word.split(delim, limit = 2).first().trim()
                        DailyStudyItem(
                            word = firstWord,
                            videoUrl = dto.videoUrl
                        )
                    }

                    // (선택) 퀴즈 화면에서 재사용할 수 있도록 메모리 캐시에 저장
                    //  - 프로젝트에 이미 존재하는 LearningMemCache 사용
                    LearningMemCache.save(
                        day = body.day,
                        items = items.map { LearningMemCache.Item(it.word, it.videoUrl) }
                    )

                    _uiState.value = DailyDetailUiState.Success(items)
                } else {
                    // HTTP 오류 → 사용자에게 보일 메시지로 가공
                    val code = res.code()
                    val msg = when (code) {
                        401 -> "인증 필요(401) – 토큰 만료/누락"
                        403 -> "권한 부족(403)"
                        else -> "HTTP $code ${res.message()}"
                    }
                    _uiState.value = DailyDetailUiState.Error(msg)
                }
            } catch (e: IOException) {
                _uiState.value = DailyDetailUiState.Error("네트워크 오류: ${e.message ?: "알 수 없음"}")
            } catch (e: Exception) {
                _uiState.value = DailyDetailUiState.Error("오류: ${e.message ?: "알 수 없음"}")
            }
        }
    }
}
