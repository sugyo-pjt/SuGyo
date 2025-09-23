package com.ssafy.a602.search

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// ───────────────────────────────────────────────────────────────────
// UI 모델
// ───────────────────────────────────────────────────────────────────
data class SearchDetailUiModel(
    val word: String,
    val description: String,
    val videoUrl: String? = null,  // 비디오 URL
    val sameMotionWords: List<String> = emptyList()
)

// ───────────────────────────────────────────────────────────────────
// ViewModel
// ───────────────────────────────────────────────────────────────────
@HiltViewModel
class SearchDetailViewModel @Inject constructor(
    private val repo: SearchRepository
) : ViewModel() {
    private var wordId: Long = 0L
    
    fun setWordId(id: Long) {
        wordId = id
        loadDetail()
    }
    private val _ui = mutableStateOf<SearchDetailUiModel?>(null)
    val ui: SearchDetailUiModel? get() = _ui.value
    
    private val _loading = mutableStateOf(true)
    val loading: Boolean get() = _loading.value
    
    private val _error = mutableStateOf<String?>(null)
    val error: String? get() = _error.value

    private fun loadDetail() {
        viewModelScope.launch {
            _loading.value = true
            val result = repo.detail(wordId)
            result.fold(
                onSuccess = { detail ->
                    _ui.value = SearchDetailUiModel(
                        word = detail.word,
                        description = detail.description,
                        videoUrl = detail.videoUrl,
                        sameMotionWords = detail.sameMotionWord
                    )
                },
                onFailure = { e ->
                    _error.value = e.message
                }
            )
            _loading.value = false
        }
    }
}

