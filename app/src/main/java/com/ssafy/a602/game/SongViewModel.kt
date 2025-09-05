package com.ssafy.a602.game

import androidx.lifecycle.ViewModel
import com.ssafy.a602.game.FakeSongs
import com.ssafy.a602.game.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SongsUiState(
    val query: String = "",
    val items: List<Song> = FakeSongs.items
) {
    val filtered: List<Song>
        get() = if (query.isBlank()) items
        else {
            val q = query.trim().lowercase()
            items.filter { it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) }
        }
}

class SongsViewModel : ViewModel() {
    private val _state = MutableStateFlow(SongsUiState())
    val state = _state.asStateFlow()
    fun onQueryChange(q: String) = _state.update { it.copy(query = q) }
}
