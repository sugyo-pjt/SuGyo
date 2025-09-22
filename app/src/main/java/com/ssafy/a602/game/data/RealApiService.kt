package com.ssafy.a602.game.data

import android.util.Log
import com.ssafy.a602.game.result.GameResultUi
import com.ssafy.a602.game.songs.SongItem
import com.ssafy.a602.game.data.SongSection
import com.ssafy.a602.game.ranking.RankingItem
import com.ssafy.a602.game.api.ApiErrorHandler
import com.ssafy.a602.game.api.RhythmApi
import javax.inject.Inject
import javax.inject.Singleton
import com.ssafy.a602.game.api.dto.ChartSegmentDto
import com.ssafy.a602.game.api.dto.MusicListItem
import com.ssafy.a602.game.api.dto.MusicUrl
import com.ssafy.a602.game.api.dto.RankingResp
import com.ssafy.a602.game.api.dto.RankingItemDto
import com.ssafy.a602.game.api.dto.MyRankingInfoDto
import com.ssafy.a602.game.utils.TimeParsing
import com.ssafy.a602.game.score.GameResultRequest
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
@Singleton
class RealApiService @Inject constructor(
    private val rhythmApi: RhythmApi
) : GameApiService {
    
    // AuthInterceptor가 자동으로 토큰을 헤더에 추가하므로 수동으로 토큰을 전달할 필요 없음
    
    override suspend fun getSongs(): List<SongItem> {
        return try {
            Log.d("RealApiService", "곡 목록 조회 시작")
            val musicList = rhythmApi.getMusicList()
            Log.d("RealApiService", "API에서 받은 곡 수: ${musicList.size}")
            
            val songItems = musicList.map { musicItem ->
                Log.d("RealApiService", "곡 변환: id=${musicItem.id}, title=${musicItem.title}")
                SongItem(
                    id = musicItem.id.toString(),
                    title = musicItem.title,
                    artist = musicItem.singer,
                    durationText = musicItem.songTime,
                    bestScore = if (musicItem.myScore != null && musicItem.myScore > 0) musicItem.myScore.toInt() else null,
                    albumImageUrl = musicItem.albumImageUrl
                )
            }
            
            Log.d("RealApiService", "곡 목록 변환 완료: ${songItems.size}개")
            songItems
        } catch (e: HttpException) {
            Log.e("RealApiService", "HTTP 에러로 곡 목록 조회 실패", e)
            handleHttpException(e)
            emptyList()
        } catch (e: IOException) {
            Log.e("RealApiService", "네트워크 에러로 곡 목록 조회 실패", e)
            handleNetworkException(e)
            emptyList()
        } catch (e: Exception) {
            Log.e("RealApiService", "일반 에러로 곡 목록 조회 실패", e)
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
            Log.d("RealApiService", "가사 및 채보 다운로드 시작: music_id=$songId")
            val chartSegments = rhythmApi.getChart(songId.toLong())
            Log.d("RealApiService", "API 응답 받음: ${chartSegments.size}개 섹션")
            
            val songSections = chartSegments.mapIndexed { index, segment ->
                Log.d("RealApiService", "섹션 ${segment.segment}: '${segment.lyrics}' (${segment.correct.size}개 정답 정보) (barEndedAt: ${segment.barEndedAt})")
                
                // correct 정보 상세 로그
                segment.correct.forEachIndexed { correctIndex, correct ->
                    Log.d("RealApiService", "  정답[$correctIndex]: 인덱스 ${correct.correctStartedIndex}~${correct.correctEndedIndex}, 액션 ${correct.actionStartedAt}~${correct.actionEndedAt}")
                }
                
                // 서비스에서 직접 변환 (TimeParsing 사용)
                val startTime = TimeParsing.toSecondsOrZero(segment.barStartedAt)
                
                // barEndedAt이 없는 경우 다음 섹션의 시작 시간을 사용하거나 기본값 설정
                val endTime = if (!segment.barEndedAt.isNullOrEmpty()) {
                    TimeParsing.toSecondsOrZero(segment.barEndedAt)
                } else {
                    // 다음 섹션의 시작 시간을 사용하거나 기본값 (1초 후)
                    val nextIndex = index + 1
                    if (nextIndex < chartSegments.size) {
                        TimeParsing.toSecondsOrZero(chartSegments[nextIndex].barStartedAt)
                    } else {
                        startTime + 1.0f // 기본값: 1초 후
                    }
                }
                
                SongSection(
                    id = segment.segment.toString(),
                    songId = songId,
                    startTime = startTime,
                    endTime = endTime,
                    text = segment.lyrics,
                    correctInfo = segment.correct
                )
            }
            
            Log.d("RealApiService", "SongSection 변환 완료: ${songSections.size}개")
            
            // 변환된 섹션들의 상세 정보 로그
            songSections.forEachIndexed { index, section ->
                Log.d("RealApiService", "섹션[$index]: '${section.text}' (${section.startTime}s~${section.endTime}s, correctInfo: ${section.correctInfo.size}개)")
            }
            
            songSections
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> {
                    Log.e("RealApiService", "곡을 찾을 수 없습니다: music_id=$songId")
                    throw RuntimeException("요청한 리소스를 찾을 수 없습니다.", e)
                }
                else -> {
                    handleHttpException(e)
                    emptyList()
                }
            }
        } catch (e: IOException) {
            Log.e("RealApiService", "네트워크 오류: ${e.message}")
            handleNetworkException(e)
            emptyList()
        } catch (e: Exception) {
            Log.e("RealApiService", "예상치 못한 오류: ${e.message}")
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
        return try {
            Log.d("RealApiService", "랭킹 조회 시작: songId=$songId")
            val musicId = songId.toLongOrNull() ?: throw IllegalArgumentException("Invalid songId: $songId")
            val response = rhythmApi.getRanking(musicId)
            
            val rankingItems = response.ranking.map { item ->
                RankingItem(
                    rank = item.rank,
                    nickname = item.userNickName,
                    score = item.score,
                    playedDate = LocalDate.parse(item.recordDate),
                    avatarUrl = item.userProfileUrl,
                    userId = item.userId.toString(),
                    isMe = false
                )
            }
            
            Log.d("RealApiService", "랭킹 조회 완료: ${rankingItems.size}개")
            rankingItems
        } catch (e: HttpException) {
            Log.e("RealApiService", "HTTP 에러로 랭킹 조회 실패", e)
            handleHttpException(e)
            emptyList()
        } catch (e: IOException) {
            Log.e("RealApiService", "네트워크 에러로 랭킹 조회 실패", e)
            handleNetworkException(e)
            emptyList()
        } catch (e: Exception) {
            Log.e("RealApiService", "일반 에러로 랭킹 조회 실패", e)
            handleGenericException(e)
            emptyList()
        }
    }
    
    override suspend fun getTop3Rankings(songId: String): List<RankingItem> {
        return try {
            Log.d("RealApiService", "Top3 랭킹 조회 시작: songId=$songId")
            val musicId = songId.toLongOrNull() ?: throw IllegalArgumentException("Invalid songId: $songId")
            val response = rhythmApi.getRanking(musicId)
            
            val top3Items = response.ranking.take(3).map { item ->
                RankingItem(
                    rank = item.rank,
                    nickname = item.userNickName,
                    score = item.score,
                    playedDate = LocalDate.parse(item.recordDate),
                    avatarUrl = item.userProfileUrl,
                    userId = item.userId.toString(),
                    isMe = false
                )
            }
            
            Log.d("RealApiService", "Top3 랭킹 조회 완료: ${top3Items.size}개")
            top3Items
        } catch (e: HttpException) {
            Log.e("RealApiService", "HTTP 에러로 Top3 랭킹 조회 실패", e)
            handleHttpException(e)
            emptyList()
        } catch (e: IOException) {
            Log.e("RealApiService", "네트워크 에러로 Top3 랭킹 조회 실패", e)
            handleNetworkException(e)
            emptyList()
        } catch (e: Exception) {
            Log.e("RealApiService", "일반 에러로 Top3 랭킹 조회 실패", e)
            handleGenericException(e)
            emptyList()
        }
    }
    
    override suspend fun getMyRanking(songId: String): RankingItem? {
        return try {
            Log.d("RealApiService", "내 랭킹 조회 시작: songId=$songId")
            val musicId = songId.toLongOrNull() ?: throw IllegalArgumentException("Invalid songId: $songId")
            val response = rhythmApi.getRanking(musicId)
            
            val myInfo = response.myInfo
            if (myInfo != null) {
                val myRanking = RankingItem(
                    rank = myInfo.rank,
                    nickname = "나", // TODO: 실제 사용자 닉네임으로 교체
                    score = myInfo.score,
                    playedDate = LocalDate.parse(myInfo.recordDate),
                    avatarUrl = null, // TODO: 실제 사용자 프로필 이미지로 교체
                    userId = null, // TODO: 실제 사용자 ID로 교체
                    isMe = true
                )
                Log.d("RealApiService", "내 랭킹 조회 완료: rank=${myInfo.rank}, score=${myInfo.score}")
                myRanking
            } else {
                Log.d("RealApiService", "내 랭킹 정보 없음")
                null
            }
        } catch (e: HttpException) {
            Log.e("RealApiService", "HTTP 에러로 내 랭킹 조회 실패", e)
            handleHttpException(e)
            null
        } catch (e: IOException) {
            Log.e("RealApiService", "네트워크 에러로 내 랭킹 조회 실패", e)
            handleNetworkException(e)
            null
        } catch (e: Exception) {
            Log.e("RealApiService", "일반 에러로 내 랭킹 조회 실패", e)
            handleGenericException(e)
            null
        }
    }
    
    override suspend fun submitRanking(songId: String, score: Int, nickname: String): RankingItem {
        // 더 이상 사용하지 않는 메서드 - 게임 완료 시 자동으로 랭킹에 반영됨
        throw UnsupportedOperationException("submitRanking은 더 이상 사용되지 않습니다. 게임 완료 시 자동으로 랭킹에 반영됩니다.")
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
    
    override suspend fun submitGameResult(result: GameResultRequest): GameResultUi {
        // 새로운 API 엔드포인트 사용 (completeGame 메서드로 대체됨)
        Log.d("RealApiService", "게임 결과 전송: ${result.songId}, 점수: ${result.totalScore}")
        
        val songs = getSongs()
        val song = songs.find { it.id == result.songId }
        
        return GameResultUi(
            songTitle = song?.title ?: "Unknown Song",
            score = result.totalScore,
            accuracyPercent = result.percent,
            grade = result.grade,
            maxCombo = result.maxCombo,
            correctCount = result.correctCount,
            missCount = result.missCount,
            comboMultiplier = when {
                result.maxCombo >= 50 -> 1.5
                result.maxCombo >= 30 -> 1.3
                result.maxCombo >= 20 -> 1.2
                result.maxCombo >= 10 -> 1.1
                else -> 1.0
            },
            isNewRecord = false,
            missWords = result.missWords,
            accepted = true,
            isPersonalBest = false, // 새로운 API에서 isBestRecord로 받아옴
            rankUpdated = false, // 새로운 API에서 처리됨
            serverScoreEcho = result.totalScore
        )
    }
    
    /**
     * 음악 URL을 가져오는 별도 메서드
     * ExoPlayer에서 사용할 수 있도록 제공
     */
    override suspend fun getMusicUrl(songId: String): String {
        return try {
            val musicUrl = rhythmApi.getMusicUrl(songId.toLong())
            val url = musicUrl.musicUrl ?: ""
            
            // 서버에서 환경변수가 치환되지 않은 경우 처리
            if (url.contains("\${SPRING_CLOUD_AWS_S3_CDN_URL}")) {
                Log.w("RealApiService", "서버에서 환경변수가 치환되지 않음. 기본 URL로 대체: $url")
                // 실제 S3 URL로 대체 (서버 설정에 따라 수정 필요)
                url.replace("\${SPRING_CLOUD_AWS_S3_CDN_URL}", "https://surocksang.s3.us-east-1.amazonaws.com")
            } else {
                url
            }
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
    
    // 시간 파싱은 TimeParsing.toMillisOrZero() 사용으로 통일됨
    
    
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
        // CancellationException은 정상적인 코루틴 취소 예외이므로 별도 처리
        if (e is kotlinx.coroutines.CancellationException) {
            Log.d("RealApiService", "코루틴 취소됨 - 정상적인 생명주기 동작")
            return // 예외를 다시 던지지 않음
        }
        
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
