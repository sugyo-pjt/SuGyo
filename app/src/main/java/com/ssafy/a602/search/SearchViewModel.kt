package com.ssafy.a602.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ───────────────────────────────────────────────────────────────────
// UI 상태 (새로운 API 스펙에 맞춤)
// ───────────────────────────────────────────────────────────────────
data class SearchUiState(
    val query: String = "",                                // 현재 입력 값
    val results: List<StudySearchItemDto> = emptyList(),  // 검색 결과 목록
    val isLoading: Boolean = false,                        // 로딩 스피너 표시 여부
    val error: String? = null,                             // 에러 메시지 (null이면 에러 없음)
    val hasSearched: Boolean = false                       // 한번이라도 검색한 상태인지
)

// ───────────────────────────────────────────────────────────────────
// ViewModel
// ───────────────────────────────────────────────────────────────────
@HiltViewModel
@OptIn(FlowPreview::class) // debounce/flatMapLatest 조합에서 필요한 표시
class SearchViewModel @Inject constructor(
    private val repo: SearchRepository
) : ViewModel() {

    // 사용자의 입력을 Flow로 받음. TextField onValueChange에서 값을 밀어 넣는다.
    private val queryFlow = MutableStateFlow("")

    // UI 상태(State)를 Flow로 보관
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow() // 외부에 read-only로 공개

    init {
        // ─ 검색 파이프라인 ─
        // 1) 사용자가 타이핑하면 queryFlow에 값이 들어옴
        // 2) 300ms(debounce) 동안 더 입력 없을 때 마지막 값만 통과
        // 3) 앞뒤 공백 제거 + 이전 값과 동일하면 무시(distinctUntilChanged)
        // 4) flatMapLatest: 이전 네트워크 요청이 진행 중이어도 새 입력이 오면 이전 요청은 취소되고 최신 요청만 유지
        viewModelScope.launch {
            queryFlow
                .debounce(300)               // 300ms 디바운스
                .map { it.trim() }           // 공백 제거
                .distinctUntilChanged()      // 같은 내용이면 다시 호출하지 않음
                .flatMapLatest { keyword ->  // 최신 입력만 유지
                    flow {
                        if (keyword.isEmpty()) {
                            // 입력이 비면 검색 결과도 비워준다
                            emit(Result.success(emptyList()))
                        } else {
                            // 네트워크 호출
                            emit(repo.search(keyword))
                        }
                    }
                }
                .collect { result ->
                    // UI 상태 갱신 (검색 결과 또는 에러)
                    result
                        .onSuccess { list -> _state.update { it.copy(results = list, error = null, hasSearched = true) } }
                        .onFailure { e    -> _state.update { it.copy(results = emptyList(), error = e.message) } }
                }
        }
    }

    // TextField 입력 이벤트 처리
    fun onQueryChange(newQuery: String) {
        _state.update { it.copy(query = newQuery, error = null) } // 화면의 query 반영 + 에러 제거
        queryFlow.value = newQuery                                // 검색 파이프라인에 새 값 푸시
    }
}

