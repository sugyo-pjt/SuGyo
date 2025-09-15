package com.ssafy.a602.game.data

import com.ssafy.a602.game.songs.SongItem
import com.ssafy.a602.game.data.SongSection
import com.ssafy.a602.game.data.SongProgress
import com.ssafy.a602.game.result.GameResultUi
import com.ssafy.a602.game.ranking.RankingItem
import com.ssafy.a602.game.score.GameResultRequest
import com.ssafy.a602.game.api.RetrofitClient
import com.ssafy.a602.game.api.dto.CompleteReq
import com.ssafy.a602.game.api.dto.CompleteResp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException

/**
 * 게임 데이터를 중앙에서 관리하는 매니저
 * 나중에 API 연동 시 이 클래스만 수정하면 됨
 */
object GameDataManager {
    
    // API 서비스들
    private val realApiService: GameApiService = RealApiService()
    private val dummyApiService: GameApiService = DummyApiService()
    
    // 더미 모드 활성화 여부 (개발/테스트용)
    private var isDummyMode = false // 기본값을 실제 API로 설정
    
    /**
     * 현재 활성화된 API 서비스 반환
     */
    private fun getCurrentApiService(): GameApiService {
        return if (isDummyMode) dummyApiService else realApiService
    }
    
    /**
     * 더미 모드 전환 (개발/테스트용)
     */
    fun setDummyMode(enabled: Boolean) {
        isDummyMode = enabled
        android.util.Log.d("GameDataManager", "API 모드: ${if (enabled) "더미 모드" else "실제 API 모드"}")
    }
    
    /**
     * 현재 더미 모드 상태 확인
     */
    fun isDummyModeEnabled(): Boolean = isDummyMode
    
    // 현재 선택된 곡
    private val _currentSong = MutableStateFlow<SongItem?>(null)
    val currentSong: StateFlow<SongItem?> = _currentSong.asStateFlow()
    
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
    fun selectSong(song: SongItem) {
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
            val currentSectionIndex = findCurrentSectionIndex(currentProgress.sections, currentTime)
            _gameProgress.value = currentProgress.copy(
                currentTime = currentTime,
                currentSectionIndex = currentSectionIndex
            )
        }
    }
    
    /**
     * 현재 시간에 해당하는 섹션 인덱스 찾기
     */
    private fun findCurrentSectionIndex(sections: List<SongSection>, currentTime: Float): Int {
        for (i in sections.indices.reversed()) {
            if (currentTime >= sections[i].startTime) {
                return i
            }
        }
        return 0
    }
    
    /**
     * 초기 게임 진행 상태 생성
     */
    private suspend fun createInitialProgress(): SongProgress? {
        val song = _currentSong.value ?: return null
        
        // API에서 소절 정보 가져오기
        val sections = createSections(song)
        
        return SongProgress(
            songId = song.id,
            currentTime = 0f,
            totalTime = parseDurationToSeconds(song.durationText),
            currentSectionIndex = 0,
            sections = sections
        )
    }
    
    /**
     * 소절 데이터 생성 (API 서비스 사용)
     */
    private suspend fun createSections(song: SongItem): List<SongSection> {
        return getCurrentApiService().getSongSections(song.id)
    }
    
    /**
     * 시간 문자열을 초로 변환 ("3:14" -> 194, "00:03:14" -> 194)
     */
    private fun parseDurationToSeconds(durationText: String): Float {
        return try {
            val parts = durationText.split(":")
            when (parts.size) {
                2 -> {
                    // MM:SS 형식
                    val minutes = parts[0].toInt()
                    val seconds = parts[1].toInt()
                    (minutes * 60 + seconds).toFloat()
                }
                3 -> {
                    // HH:MM:SS 형식
                    val hours = parts[0].toInt()
                    val minutes = parts[1].toInt()
                    val seconds = parts[2].toInt()
                    (hours * 3600 + minutes * 60 + seconds).toFloat()
                }
                else -> 180f // 기본값
            }
        } catch (e: Exception) {
            180f // 기본값
        }
    }
    
    /**
     * 현재 곡의 소절 정보 가져오기
     */
    suspend fun getSongSections(songId: String): List<SongSection> {
        return getCurrentApiService().getSongSections(songId)
    }
    
    /**
     * 곡 목록 가져오기
     */
    suspend fun getSongs(): List<SongItem> {
        return getCurrentApiService().getSongs()
    }
    
    /**
     * 곡 검색
     */
    suspend fun searchSongs(query: String): List<SongItem> {
        return getCurrentApiService().searchSongs(query)
    }
    
    /**
     * 음악 URL 가져오기 (ExoPlayer용)
     */
    suspend fun getMusicUrl(songId: String): String? {
        return try {
            getCurrentApiService().getMusicUrl(songId)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 곡 ID로 곡 찾기
     */
    suspend fun getSongById(songId: String): SongItem? {
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
        return getCurrentApiService().calculateGameResult(songId, score, correctCount, missCount, maxCombo, missWords)
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
        val bestScore = getCurrentApiService().getUserBestScore(songId)
        return bestScore == null || score > bestScore
    }
    
    /**
     * 특정 곡의 순위 목록 가져오기
     */
    suspend fun getRankings(songId: String): List<RankingItem> {
        return getCurrentApiService().getRankings(songId)
    }
    
    /**
     * 특정 곡의 Top 3 순위 가져오기
     */
    suspend fun getTop3Rankings(songId: String): List<RankingItem> {
        return getCurrentApiService().getTop3Rankings(songId)
    }
    
    /**
     * 내 순위 가져오기
     */
    suspend fun getMyRanking(songId: String): RankingItem? {
        return getCurrentApiService().getMyRanking(songId)
    }
    
    /**
     * 게임 결과 전송 (프론트에서 계산된 결과)
     */
    suspend fun submitGameResult(req: GameResultRequest): Result<GameResultUi> = try {
        Result.success(getCurrentApiService().submitGameResult(req))
    } catch (ce: CancellationException) {
        throw ce
    } catch (t: Throwable) {
        Result.failure(t)
    }
    
    /**
     * 게임 완료 결과 전송 (새로운 API)
     */
    suspend fun completeGame(musicId: Long, score: Int): Result<CompleteResp> = try {
        val request = CompleteReq(musicId = musicId, score = score)
        // TODO: 로그인 구현 후 실제 토큰으로 교체
        val response = RetrofitClient.rhythmApi.complete("Bearer test_token_for_development", request)
        Result.success(response)
    } catch (ce: CancellationException) {
        throw ce
    } catch (t: Throwable) {
        Result.failure(t)
    }
    
    /**
     * 개발/테스트용 유틸리티 함수들
     */
    object DevUtils {
        /**
         * 더미 모드로 전환 (개발/테스트용)
         */
        fun enableDummyMode() {
            setDummyMode(true)
            android.util.Log.d("GameDataManager", "🔧 개발 모드: 더미 데이터 사용")
        }
        
        /**
         * 실제 API 모드로 전환 (운영용)
         */
        fun enableRealApiMode() {
            setDummyMode(false)
            android.util.Log.d("GameDataManager", "🚀 운영 모드: 실제 API 사용")
        }
        
        /**
         * 현재 모드 상태 출력
         */
        fun printCurrentMode() {
            val mode = if (isDummyModeEnabled()) "더미 모드" else "실제 API 모드"
            android.util.Log.d("GameDataManager", "현재 모드: $mode")
        }
    }
}
