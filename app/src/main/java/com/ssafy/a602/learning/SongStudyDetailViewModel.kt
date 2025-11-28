package com.ssafy.a602.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a602.learning.api.StudyApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

/* ──────────────────────────────────────────────────────────────────────────
 * 화면에 뿌리기 쉬운 UI 전용 모델
 * ────────────────────────────────────────────────────────────────────────── */
data class SongWordItem(
    val word: String,
    val videoUrl: String? = null
)

/* ──────────────────────────────────────────────────────────────────────────
 * 화면 상태 (로딩/성공/에러)
 * ────────────────────────────────────────────────────────────────────────── */
sealed interface SongStudyDetailUiState {
    data object Loading : SongStudyDetailUiState
    data class Success(
        val songTitle: String,
        val words: List<SongWordItem>
    ) : SongStudyDetailUiState
    data class Error(val message: String) : SongStudyDetailUiState
}

/* ──────────────────────────────────────────────────────────────────────────
 * ViewModel
 * ────────────────────────────────────────────────────────────────────────── */
@HiltViewModel
class SongStudyDetailViewModel @Inject constructor(
    private val studyApiService: StudyApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<SongStudyDetailUiState>(SongStudyDetailUiState.Loading)
    val uiState: StateFlow<SongStudyDetailUiState> = _uiState

    /** 곡 ID와 제목으로 상세 데이터 로드 */
    fun load(songId: String, songTitle: String = "노래 학습") {
        Log.d("SongStudyDetailVM", "load() 호출: songId=$songId")
        _uiState.value = SongStudyDetailUiState.Loading

        viewModelScope.launch {
            try {
                // 노래 학습 API로 단어 목록 가져오기
                val musicId = songId.toInt()
                Log.d("SongStudyDetailVM", "API 호출: /api/v1/study/music/$musicId")
                
                val response = studyApiService.getSongStudy(musicId)

                if (response.isSuccessful && response.body() != null) {
                    val songItemsResponse = response.body()!!
                    Log.d("SongStudyDetailVM", "API 응답 성공: musicId=${musicId}, 단어 수=${songItemsResponse.size}")
                    
                    // API에서 받은 단어들을 SongWordItem으로 변환
                    val wordItems = songItemsResponse.map { dto ->
                        Log.d("SongStudyDetailVM", "단어: ${dto.word}, 비디오 URL: ${dto.videoUrl}")
                        SongWordItem(
                            word = dto.word,
                            videoUrl = dto.videoUrl
                        )
                    }
                    
                    _uiState.value = SongStudyDetailUiState.Success(
                        songTitle = songTitle,  // 전달받은 곡 제목 사용
                        words = wordItems
                    )
                } else {
                    Log.e("SongStudyDetailVM", "API 응답 실패: ${response.code()} ${response.message()}")
                    _uiState.value = SongStudyDetailUiState.Error("서버에서 단어 목록을 가져올 수 없습니다. (${response.code()})")
                }
            } catch (e: Exception) {
                Log.e("SongStudyDetailVM", "데이터 로드 실패", e)
                _uiState.value = SongStudyDetailUiState.Error("데이터 로드에 실패했습니다: ${e.message}")
            }
        }
    }
}