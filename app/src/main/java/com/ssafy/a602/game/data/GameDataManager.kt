package com.ssafy.a602.game.data

import com.ssafy.a602.game.Song
import com.ssafy.a602.game.SongSection
import com.ssafy.a602.game.SongProgress
import com.ssafy.a602.game.GameResultUi
import com.ssafy.a602.game.RankingItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 게임 데이터를 중앙에서 관리하는 매니저
 * 나중에 API 연동 시 이 클래스만 수정하면 됨
 */
object GameDataManager {
    
    // API 서비스 (더미 구현체 사용, 실제 API 연동 시 교체)
    // TODO: API 연동 시 아래 한 줄만 바꾸면 됨
    // private val apiService: GameApiService = RealApiService()
    private val apiService: GameApiService = DummyGameApiService()
    
    // 현재 선택된 곡
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()
    
    // 현재 게임 진행 상태
    private val _gameProgress = MutableStateFlow<SongProgress?>(null)
    val gameProgress: StateFlow<SongProgress?> = _gameProgress.asStateFlow()
    
    // 게임 상태
    private val _isGameActive = MutableStateFlow(false)
    val isGameActive: StateFlow<Boolean> = _isGameActive.asStateFlow()
    
    // 최근 게임 결과
    private val _lastGameResult = MutableStateFlow<GameResultUi?>(null)
    val lastGameResult: StateFlow<GameResultUi?> = _lastGameResult.asStateFlow()
    
    /**
     * 곡 선택
     */
    fun selectSong(song: Song) {
        _currentSong.value = song
        _isGameActive.value = false
        _gameProgress.value = null
    }
    
    /**
     * 게임 시작
     */
    suspend fun startGame() {
        _isGameActive.value = true
        _gameProgress.value = createInitialProgress()
    }
    
    /**
     * 게임 종료
     */
    fun endGame() {
        _isGameActive.value = false
        _gameProgress.value = null
    }
    
    /**
     * 게임 결과 저장
     */
    fun saveGameResult(result: GameResultUi) {
        _lastGameResult.value = result
    }
    
    /**
     * 게임 진행 상태 업데이트
     */
    fun updateGameProgress(currentTime: Float) {
        _gameProgress.value?.let { currentProgress ->
            _gameProgress.value = currentProgress.copy(currentTime = currentTime)
        }
    }
    
    /**
     * 초기 게임 진행 상태 생성
     */
    private suspend fun createInitialProgress(): SongProgress? {
        val song = _currentSong.value ?: return null
        
        // API에서 소절 정보 가져오기
        val sections = createSections(song)
        
        return SongProgress(
            currentTime = 0f,
            totalDuration = parseDurationToSeconds(song.durationText),
            sections = sections
        )
    }
    
    /**
     * 소절 데이터 생성 (API 서비스 사용)
     */
    private suspend fun createSections(song: Song): List<SongSection> {
        return apiService.getSongSections(song.id)
    }
    
    /**
     * 시간 문자열을 초로 변환 ("3:14" -> 194)
     */
    private fun parseDurationToSeconds(durationText: String): Float {
        return try {
            val parts = durationText.split(":")
            val minutes = parts[0].toInt()
            val seconds = parts[1].toInt()
            (minutes * 60 + seconds).toFloat()
        } catch (e: Exception) {
            180f // 기본값
        }
    }
    
    /**
     * 현재 곡의 소절 정보 가져오기
     */
    suspend fun getSongSections(songId: String): List<SongSection> {
        return apiService.getSongSections(songId)
    }
    
    /**
     * 곡 목록 가져오기
     */
    suspend fun getSongs(): List<Song> {
        return apiService.getSongs()
    }
    
    /**
     * 곡 검색
     */
    suspend fun searchSongs(query: String): List<Song> {
        return apiService.searchSongs(query)
    }
    
    /**
     * 곡 ID로 곡 찾기
     */
    suspend fun getSongById(songId: String): Song? {
        val songs = getSongs()
        return songs.find { it.id == songId }
    }
    
    /**
     * 현재 선택된 곡이 주어진 ID와 일치하는지 확인
     */
    fun isCurrentSong(songId: String): Boolean {
        return _currentSong.value?.id == songId
    }
    
    /**
     * 게임 결과 생성 (백엔드에서 계산된 결과 사용)
     * TODO: API 연동 시 백엔드에서 계산된 결과를 받아와서 사용
     */
    suspend fun createGameResult(
        songId: String,
        score: Int,
        correctCount: Int,
        missCount: Int,
        maxCombo: Int,
        missWords: List<String>
    ): GameResultUi {
        val song = getSongById(songId) ?: throw IllegalArgumentException("Song not found: $songId")
        
        // 백엔드에서 계산된 결과를 받아와서 사용
        return apiService.calculateGameResult(songId, score, correctCount, missCount, maxCombo, missWords)
    }
    
    /**
     * 콤보 배율 계산
     */
    private fun calculateComboMultiplier(maxCombo: Int): Double {
        return when {
            maxCombo >= 50 -> 1.5
            maxCombo >= 30 -> 1.3
            maxCombo >= 20 -> 1.2
            maxCombo >= 10 -> 1.1
            else -> 1.0
        }
    }
    
    /**
     * 등급 계산
     */
    private fun calculateGrade(accuracyPercent: Int): String {
        return when {
            accuracyPercent >= 95 -> "S"
            accuracyPercent >= 85 -> "A"
            accuracyPercent >= 70 -> "B"
            accuracyPercent >= 50 -> "C"
            else -> "F"
        }
    }
    
    /**
     * 신기록 여부 확인
     */
    private suspend fun checkNewRecord(songId: String, score: Int): Boolean {
        val bestScore = apiService.getUserBestScore(songId)
        return bestScore == null || score > bestScore
    }
    
    /**
     * 특정 곡의 순위 목록 가져오기
     */
    suspend fun getRankings(songId: String): List<RankingItem> {
        return apiService.getRankings(songId)
    }
    
    /**
     * 특정 곡의 Top 3 순위 가져오기
     */
    suspend fun getTop3Rankings(songId: String): List<RankingItem> {
        return apiService.getTop3Rankings(songId)
    }
    
    /**
     * 내 순위 가져오기
     */
    suspend fun getMyRanking(songId: String): RankingItem? {
        return apiService.getMyRanking(songId)
    }
}
