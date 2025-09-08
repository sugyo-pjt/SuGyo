package com.ssafy.a602.game.data

import com.ssafy.a602.game.Song
import com.ssafy.a602.game.SongSection
import com.ssafy.a602.game.SongProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 게임 데이터를 중앙에서 관리하는 매니저
 * 나중에 API 연동 시 이 클래스만 수정하면 됨
 */
object GameDataManager {
    
    // API 서비스 (더미 구현체 사용, 실제 API 연동 시 교체)
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
}
