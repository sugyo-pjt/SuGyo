package com.ssafy.a602.game.data

import com.ssafy.a602.game.GameResultUi
import com.ssafy.a602.game.Song
import com.ssafy.a602.game.SongSection
import com.ssafy.a602.game.RankingItem
import java.time.LocalDate

/**
 * 실제 API 연동을 위한 서비스 구현체 예시
 * 
 * 실제 백엔드 API와 연동할 때 이 클래스를 구현하면 됩니다.
 * 
 * 사용 방법:
 * 1. GameDataManager.kt에서 DummyGameApiService()를 RealApiService()로 변경
 * 2. 아래 메서드들을 실제 API 호출로 구현
 * 3. 네트워크 라이브러리 (Retrofit, OkHttp 등) 추가
 */
class RealApiService : GameApiService {
    
    // TODO: 실제 API 클라이언트 초기화
    // private val apiClient = RetrofitClient.create()
    
    override suspend fun getSongs(): List<Song> {
        // TODO: 실제 API 호출
        // return apiClient.getSongs()
        
        // 예시 구현
        return emptyList()
    }
    
    override suspend fun searchSongs(query: String): List<Song> {
        // TODO: 실제 API 호출
        // return apiClient.searchSongs(query)
        
        // 예시 구현
        return emptyList()
    }
    
    override suspend fun getSongSections(songId: String): List<SongSection> {
        // TODO: 실제 API 호출
        // return apiClient.getSongSections(songId)
        
        // 예시 구현
        return emptyList()
    }
    
    override suspend fun saveGameResult(songId: String, score: Int, accuracy: Float) {
        // TODO: 실제 API 호출
        // apiClient.saveGameResult(songId, score, accuracy)
        
        // 예시 구현
    }
    
    override suspend fun getUserBestScore(songId: String): Int? {
        // TODO: 실제 API 호출
        // return apiClient.getUserBestScore(songId)
        
        // 예시 구현
        return null
    }
    
    override suspend fun getRankings(songId: String): List<RankingItem> {
        // TODO: 실제 API 호출
        // return apiClient.getRankings(songId)
        
        // 예시 구현
        return emptyList()
    }
    
    override suspend fun getTop3Rankings(songId: String): List<RankingItem> {
        // TODO: 실제 API 호출
        // return apiClient.getTop3Rankings(songId)
        
        // 예시 구현
        return emptyList()
    }
    
    override suspend fun getMyRanking(songId: String): RankingItem? {
        // TODO: 실제 API 호출
        // return apiClient.getMyRanking(songId)
        
        // 예시 구현
        return null
    }
    
    override suspend fun calculateGameResult(
        songId: String,
        score: Int,
        correctCount: Int,
        missCount: Int,
        maxCombo: Int,
        missWords: List<String>
    ): GameResultUi {
        // TODO: 실제 API 호출
        // return apiClient.calculateGameResult(songId, score, correctCount, missCount, maxCombo, missWords)
        
        // 예시 구현
        return GameResultUi(
            songTitle = "Unknown Song",
            score = score,
            accuracyPercent = 0,
            grade = "F",
            maxCombo = maxCombo,
            correctCount = correctCount,
            missCount = missCount,
            comboMultiplier = 1.0,
            isNewRecord = false,
            missWords = missWords
        )
    }
}

/**
 * Retrofit API 인터페이스 예시
 * 
 * 실제 API 연동 시 사용할 수 있는 Retrofit 인터페이스 예시입니다.
 */
/*
interface GameApi {
    
    @GET("songs")
    suspend fun getSongs(): List<Song>
    
    @GET("songs/search")
    suspend fun searchSongs(@Query("q") query: String): List<Song>
    
    @GET("songs/{songId}/sections")
    suspend fun getSongSections(@Path("songId") songId: String): List<SongSection>
    
    @POST("game/result")
    suspend fun saveGameResult(@Body request: SaveGameResultRequest): Response<Unit>
    
    @GET("user/best-score/{songId}")
    suspend fun getUserBestScore(@Path("songId") songId: String): Int?
    
    @GET("rankings/{songId}")
    suspend fun getRankings(@Path("songId") songId: String): List<RankingItem>
    
    @GET("rankings/{songId}/top3")
    suspend fun getTop3Rankings(@Path("songId") songId: String): List<RankingItem>
    
    @GET("user/ranking/{songId}")
    suspend fun getMyRanking(@Path("songId") songId: String): RankingItem?
    
    @POST("game/calculate-result")
    suspend fun calculateGameResult(@Body request: CalculateGameResultRequest): GameResultUi
}

data class SaveGameResultRequest(
    val songId: String,
    val score: Int,
    val accuracy: Float
)

data class CalculateGameResultRequest(
    val songId: String,
    val score: Int,
    val correctCount: Int,
    val missCount: Int,
    val maxCombo: Int,
    val missWords: List<String>
)
*/
