package com.ssafy.a602.game.data

import com.ssafy.a602.game.result.GameResultUi
import com.ssafy.a602.game.songs.SongItem
import com.ssafy.a602.game.data.SongSection
import com.ssafy.a602.game.ranking.RankingItem
import com.ssafy.a602.game.api.ApiErrorHandler
import com.ssafy.a602.game.api.RetrofitClient
import com.ssafy.a602.game.api.dto.ChartSegment
import com.ssafy.a602.game.api.dto.MusicListItem
import com.ssafy.a602.game.api.dto.MusicUrl
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate

/**
 * 실제 API 연동을 위한 서비스 구현체
 * 
 * 제공된 API 스펙에 맞춰 구현된 실제 API 서비스
 * 
 * 사용 방법:
 * 1. GameDataManager.kt에서 DummyGameApiService()를 RealApiService()로 변경
 * 2. RetrofitClient의 BASE_URL을 실제 서버 URL로 변경
 * 3. 인증 토큰을 적절히 관리
 */
class RealApiService : GameApiService {
    
    private val rhythmApi = RetrofitClient.rhythmApi
    
    // TODO: 실제 인증 토큰 관리 로직 구현
    private fun getAuthToken(): String {
        // 실제로는 SharedPreferences나 다른 저장소에서 토큰을 가져와야 함
        return "Bearer your_access_token_here"
    }
    
    override suspend fun getSongs(): List<SongItem> {
        return try {
            val musicList = rhythmApi.getMusicList(getAuthToken())
            musicList.map { musicItem ->
                SongItem(
                    id = musicItem.id.toString(),
                    title = musicItem.title,
                    artist = musicItem.singer,
                    durationText = musicItem.songTime,
                    bestScore = if (musicItem.myScore > 0) musicItem.myScore.toInt() else null,
                    albumImageUrl = musicItem.albumImageUrl
                )
            }
        } catch (e: HttpException) {
            handleHttpException(e)
            emptyList()
        } catch (e: IOException) {
            handleNetworkException(e)
            emptyList()
        } catch (e: Exception) {
            handleGenericException(e)
            emptyList()
        }
    }
    
    override suspend fun searchSongs(query: String): List<SongItem> {
        // API 스펙에 검색 기능이 없으므로 클라이언트 사이드에서 필터링
        val allSongs = getSongs()
        return allSongs.filter { song ->
            song.title.contains(query, ignoreCase = true) ||
            song.artist.contains(query, ignoreCase = true)
        }
    }
    
    override suspend fun getSongSections(songId: String): List<SongSection> {
        return try {
            val chartSegments = rhythmApi.getChart(getAuthToken(), songId.toLong())
            chartSegments.map { segment ->
                SongSection(
                    id = segment.segment.toString(),
                    songId = songId,
                    startTime = parseTimeToSeconds(segment.barStartedAt),
                    endTime = parseTimeToSeconds(segment.barStartedAt) + calculateSegmentDuration(segment),
                    text = segment.lyrics
                )
            }
        } catch (e: HttpException) {
            handleHttpException(e)
            emptyList()
        } catch (e: IOException) {
            handleNetworkException(e)
            emptyList()
        } catch (e: Exception) {
            handleGenericException(e)
            emptyList()
        }
    }
    
    override suspend fun saveGameResult(result: GameResultUi): Boolean {
        // 백엔드에서 구현되지 않은 기능 - 비워둠
        return true
    }
    
    override suspend fun getUserBestScore(songId: String): Int? {
        // API 스펙에 개별 점수 조회 기능이 없으므로 곡 목록에서 가져옴
        val songs = getSongs()
        return songs.find { it.id == songId }?.bestScore
    }
    
    override suspend fun getRankings(songId: String): List<RankingItem> {
        // 백엔드에서 구현되지 않은 기능 - 비워둠
        return emptyList()
    }
    
    override suspend fun getTop3Rankings(songId: String): List<RankingItem> {
        // 백엔드에서 구현되지 않은 기능 - 비워둠
        return emptyList()
    }
    
    override suspend fun getMyRanking(songId: String): RankingItem? {
        // 백엔드에서 구현되지 않은 기능 - 비워둠
        return null
    }
    
    override suspend fun submitRanking(songId: String, score: Int, nickname: String): RankingItem {
        // 백엔드에서 구현되지 않은 기능 - 기본값 반환
        return RankingItem(
            rank = 1,
            nickname = nickname,
            score = score,
            playedDate = java.time.LocalDate.now()
        )
    }
    
    override suspend fun calculateGameResult(
        songId: String,
        score: Int,
        correctCount: Int,
        missCount: Int,
        maxCombo: Int,
        missWords: List<String>
    ): GameResultUi {
        // 백엔드에서 구현되지 않은 기능 - 기본값만 반환
        val songs = getSongs()
        val song = songs.find { it.id == songId }
        
        return GameResultUi(
            songTitle = song?.title ?: "Unknown Song",
            score = score,
            accuracyPercent = if (correctCount + missCount > 0) (correctCount * 100 / (correctCount + missCount)) else 0,
            grade = when {
                correctCount + missCount == 0 -> "F"
                (correctCount * 100 / (correctCount + missCount)) >= 95 -> "S"
                (correctCount * 100 / (correctCount + missCount)) >= 85 -> "A"
                (correctCount * 100 / (correctCount + missCount)) >= 70 -> "B"
                (correctCount * 100 / (correctCount + missCount)) >= 50 -> "C"
                else -> "F"
            },
            maxCombo = maxCombo,
            correctCount = correctCount,
            missCount = missCount,
            comboMultiplier = when {
                maxCombo >= 50 -> 1.5
                maxCombo >= 30 -> 1.3
                maxCombo >= 20 -> 1.2
                maxCombo >= 10 -> 1.1
                else -> 1.0
            },
            isNewRecord = false, // 백엔드에서 계산 필요
            missWords = missWords
        )
    }
    
    /**
     * 음악 URL을 가져오는 별도 메서드
     * ExoPlayer에서 사용할 수 있도록 제공
     */
    override suspend fun getMusicUrl(songId: String): String {
        return try {
            val musicUrl = rhythmApi.getMusicUrl(getAuthToken(), songId.toLong())
            musicUrl.musicUrl ?: ""
        } catch (e: HttpException) {
            handleHttpException(e)
            ""
        } catch (e: IOException) {
            handleNetworkException(e)
            ""
        } catch (e: Exception) {
            handleGenericException(e)
            ""
        }
    }
    
    // ========== 유틸리티 함수들 ==========
    
    /**
     * 시간 문자열을 초로 변환 ("HH:MM:SS.xx" -> Float)
     */
    private fun parseTimeToSeconds(timeString: String): Float {
        return try {
            val parts = timeString.split(":")
            val hours = parts[0].toInt()
            val minutes = parts[1].toInt()
            val secondsWithMs = parts[2].toFloat()
            (hours * 3600 + minutes * 60 + secondsWithMs)
        } catch (e: Exception) {
            0f
        }
    }
    
    /**
     * 섹션의 지속 시간 계산
     */
    private fun calculateSegmentDuration(segment: ChartSegment): Float {
        return if (segment.correct.isNotEmpty()) {
            val firstCorrect = segment.correct.first()
            val startTime = parseTimeToSeconds(firstCorrect.actionStartedAt)
            val endTime = parseTimeToSeconds(firstCorrect.actionEndedAt)
            endTime - startTime
        } else {
            1.0f // 기본값
        }
    }
    
    // ========== 에러 처리 함수들 ==========
    
    private fun handleHttpException(e: HttpException) {
        val message = ApiErrorHandler.handleHttpError(e)
        ApiErrorHandler.logError("RealApiService", "HTTP 에러 발생", e)
        throw RuntimeException(message, e)
    }
    
    private fun handleNetworkException(e: IOException) {
        val message = ApiErrorHandler.handleNetworkError(e)
        ApiErrorHandler.logError("RealApiService", "네트워크 에러 발생", e)
        throw RuntimeException(message, e)
    }
    
    private fun handleGenericException(e: Exception) {
        val message = ApiErrorHandler.handleGenericError(e)
        ApiErrorHandler.logError("RealApiService", "일반 에러 발생", e)
        throw RuntimeException(message, e)
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
    suspend fun getSongs(): List<SongItem>
    
    @GET("songs/search")
    suspend fun searchSongs(@Query("q") query: String): List<SongItem>
    
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
