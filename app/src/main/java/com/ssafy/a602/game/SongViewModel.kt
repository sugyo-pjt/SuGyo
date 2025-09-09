package com.ssafy.a602.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a602.game.data.GameDataManager
import com.ssafy.a602.game.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SongsUiState(
    val query: String = "",
    val items: List<Song> = emptyList(),
    val isLoading: Boolean = false
) {
    val filtered: List<Song>
        get() = items // 검색은 서버에서 처리
}

class SongsViewModel : ViewModel() {
    private val _state = MutableStateFlow(SongsUiState())
    val state = _state.asStateFlow()
    
    init {
        loadSongs()
    }
    
    fun onQueryChange(q: String) {
        _state.update { it.copy(query = q) }
        searchSongs(q)
    }
    
    /**
     * 곡 목록 로드
     */
    private fun loadSongs() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val songs = GameDataManager.getSongs()
                _state.update { it.copy(items = songs, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                // 에러 처리
            }
        }
    }
    
    /**
     * 곡 검색
     */
    private fun searchSongs(query: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val songs = GameDataManager.searchSongs(query)
                _state.update { it.copy(items = songs, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                // 에러 처리
            }
        }
    }
    
    /**
     * 곡 선택 시 GameDataManager에 저장
     */
    fun selectSong(song: Song) {
        GameDataManager.selectSong(song)
    }
}
