package com.ssafy.a602.term.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a602.term.data.TermRepository
import com.ssafy.a602.term.data.model.TermDetailDto
import com.ssafy.a602.term.data.model.TermSummaryDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 약관 화면 ViewModel
 * 
 * 약관 목록 및 상세 정보 관리
 */
@HiltViewModel
class TermViewModel @Inject constructor(
    private val repository: TermRepository
) : ViewModel() {
    
    // 약관 목록 상태
    private val _listState = MutableStateFlow(TermsUiState())
    val listState: StateFlow<TermsUiState> = _listState.asStateFlow()
    
    // 약관 상세 상태
    private val _detailState = MutableStateFlow(TermDetailUiState())
    val detailState: StateFlow<TermDetailUiState> = _detailState.asStateFlow()
    
    /**
     * 약관 요약 목록 로드
     */
    fun loadSummaries() {
        _listState.value = _listState.value.copy(loading = true, error = null)
        viewModelScope.launch {
            repository.fetchSummaries()
                .onSuccess { summaries ->
                    _listState.value = TermsUiState(
                        loading = false,
                        items = summaries,
                        error = null
                    )
                }
                .onFailure { exception ->
                    _listState.value = TermsUiState(
                        loading = false,
                        items = emptyList(),
                        error = exception.message ?: "약관 목록을 불러오는데 실패했습니다."
                    )
                }
        }
    }
    
    /**
     * 약관 상세 정보 로드
     * @param id 약관 ID
     */
    fun loadDetail(id: Long) {
        _detailState.value = TermDetailUiState(loading = true, error = null)
        viewModelScope.launch {
            repository.fetchDetail(id)
                .onSuccess { detail ->
                    _detailState.value = TermDetailUiState(
                        loading = false,
                        detail = detail,
                        error = null
                    )
                }
                .onFailure { exception ->
                    _detailState.value = TermDetailUiState(
                        loading = false,
                        detail = null,
                        error = exception.message ?: "약관 상세 정보를 불러오는데 실패했습니다."
                    )
                }
        }
    }
    
    /**
     * 목록 에러 상태 초기화
     */
    fun clearListError() {
        _listState.value = _listState.value.copy(error = null)
    }
    
    /**
     * 상세 에러 상태 초기화
     */
    fun clearDetailError() {
        _detailState.value = _detailState.value.copy(error = null)
    }
}

/**
 * 약관 목록 UI 상태
 */
data class TermsUiState(
    val loading: Boolean = false,
    val items: List<TermSummaryDto> = emptyList(),
    val error: String? = null
)

/**
 * 약관 상세 UI 상태
 */
data class TermDetailUiState(
    val loading: Boolean = false,
    val detail: TermDetailDto? = null,
    val error: String? = null
)
