package com.ssafy.a602.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a602.learning.api.DayItemDto
import com.ssafy.a602.learning.api.QuizResultRequest
import com.ssafy.a602.learning.api.StudyApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// 퀴즈 1문항 모델
data class Question(
    val wordId: Long,
    val videoUrl: String?,
    val options: List<String>,  // 섞인 보기
    val answer: String          // 정답(=options 중 하나)
)

// UI 상태
sealed interface QuizUiState {
    data object Loading : QuizUiState
    data class Ready(val questions: List<Question>) : QuizUiState
    data class Error(val message: String) : QuizUiState
}

@HiltViewModel
class DailyQuizViewModel @Inject constructor(
    private val api: StudyApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Loading)
    val uiState: StateFlow<QuizUiState> = _uiState

    /** 마지막으로 불러온 dayId(=서버에 보낼 값). load(dayId) 호출 시 보관 */
    private var lastRequestedDayId: Int? = null

    /**
     * 일차 상세 불러오기
     * @param dayId 서버에 Path로 보내는 값(백엔드 스펙이 day 번호=PK라면 이 값이 그대로 전송 대상)
     */
    fun load(dayId: Int) {
        lastRequestedDayId = dayId
        _uiState.value = QuizUiState.Loading

        viewModelScope.launch {
            try {
                val res = api.getDayDetail(dayId)
                if (!res.isSuccessful) {
                    _uiState.value = QuizUiState.Error("HTTP ${res.code()} ${res.message()}")
                    return@launch
                }
                val body = res.body() ?: run {
                    _uiState.value = QuizUiState.Error("응답 본문이 비어 있습니다.")
                    return@launch
                }

                // DayItemDto → Question 리스트
                val questions = buildQuestionsFromItems(body.items, optionsPerQuestion = 4)
                _uiState.value = QuizUiState.Ready(questions)
            } catch (t: Throwable) {
                _uiState.value = QuizUiState.Error(t.message ?: "알 수 없는 오류")
            }
        }
    }

    /**
     * 퀴즈 결과 전송 (dayId 명시형)
     * @param dayId POST 바디의 dayId
     * @param score 맞춘 개수
     */
    suspend fun submitResult(dayId: Int, score: Int): Boolean {
        return try {
            val res = api.postQuizResult(QuizResultRequest(dayId = dayId, score = score))
            res.isSuccessful
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * 퀴즈 결과 전송 (load 이후 저장된 dayId 사용)
     * @param score 맞춘 개수
     */
    suspend fun submitResult(score: Int): Boolean {
        val id = lastRequestedDayId ?: return false
        return submitResult(dayId = id, score = score)
    }

    /** "안녕하세요,안녕히 계세요" → "안녕하세요" 처럼 대표어만 추출 */
    private fun primaryLabel(raw: String): String =
        raw.split(',', '·', '/', ';').firstOrNull()?.trim().orEmpty()

    /** 서버 항목 → 문항 리스트 (정답=해당 항목, 오답=다른 wordId 중 랜덤 3개) */
    private fun buildQuestionsFromItems(
        items: List<DayItemDto>,
        optionsPerQuestion: Int = 4
    ): List<Question> {
        if (items.isEmpty()) return emptyList()

        val optCount = optionsPerQuestion.coerceIn(2, items.size)

        return items.shuffled().map { correct ->
            val correctText = primaryLabel(correct.word)

            // 오답 풀(정답 wordId 제외) → 대표어로 바꾸고 중복 제거
            val distractorPool = items
                .filter { it.wordId != correct.wordId }
                .map { primaryLabel(it.word) }
                .distinct()

            // 오답 (optCount-1)개 랜덤 추출
            val needed = (optCount - 1).coerceAtLeast(1)
            val distractors = distractorPool.shuffled().take(needed)

            // 보기 = 오답 + 정답 → 섞기, 중복 방지 최종 확인
            val options = (distractors + correctText)
                .distinct()
                .shuffled()

            Question(
                wordId   = correct.wordId,
                videoUrl = correct.videoUrl,
                options  = options,
                answer   = correctText
            )
        }
    }
}
