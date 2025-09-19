package com.ssafy.a602.game.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a602.game.data.GameDataManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 게임 순위 화면의 ViewModel
 */
class GameRankingViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(RankingUiState())
    val uiState: StateFlow<RankingUiState> = _uiState.asStateFlow()
    
    /**
     * 순위 데이터 로드
     */
    fun loadRankings(songId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // 랭킹 정보 가져오기 (곡 제목과 전체 순위)
                val (songTitle, allRankings) = GameDataManager.getRankingInfo(songId)
                
                // Top 3 순위 가져오기
                val top3Rankings = GameDataManager.getTop3Rankings(songId)
                
                // 내 순위 가져오기
                val myRanking = GameDataManager.getMyRanking(songId)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    songTitle = songTitle,
                    top3Rankings = top3Rankings,
                    allRankings = allRankings,
                    myRanking = myRanking
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "순위를 불러오는데 실패했습니다."
                )
            }
        }
    }
    
    /**
     * 에러 상태 초기화
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
