package com.ssafy.a602.learning.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Body

// ───────────────────────────────────────────────────────────────
// [1] 메인 화면: 진행도 요약
// ───────────────────────────────────────────────────────────────
data class ProgressResponse(
    val progressDay: Int
)

// ───────────────────────────────────────────────────────────────
// [2] 로드맵 화면: 진행도 상세
// ───────────────────────────────────────────────────────────────
data class ProgressDetailResponse(
    val totalDays: Int,
    val progressDay: Int,
    val days: List<DayDetail> = emptyList()
)

data class DayDetail(
    val dayId: Long? = null,
    val day: Int,
    val correctCount: Int? = null,
    val totalCount: Int
)

// ───────────────────────────────────────────────────────────────
// [3] 일차 상세: 단어/영상 목록
// ───────────────────────────────────────────────────────────────
data class DayItemsResponse(
    val day: Int,
    val items: List<DayItemDto>
)

data class DayItemDto(
    val wordId: Long,
    val word: String,
    val description: String?,
    val videoUrl: String?
)

// ───────────────────────────────────────────────────────────────
// [4] 퀴즈 결과 저장
// ───────────────────────────────────────────────────────────────
data class QuizResultRequest(
    val dayId: Int,
    val score: Int
)

@JvmInline
value class EmptyBody private constructor(val nothing: String = "")

// ───────────────────────────────────────────────────────────────
// [5] 노래학습 목록 조회 (✔️ 이게 핵심: /api/v1/music/study)
// ───────────────────────────────────────────────────────────────
data class MusicStudyListResponse(
    val musics: List<MusicStudyItem>
)

data class MusicStudyItem(
    val musicId: Int,
    val title: String,
    val singer: String,
    val albumImageUrl: String?,
    val countWord: Int
)

// ───────────────────────────────────────────────────────────────
// [6] 노래학습 상세(단어/영상)
// ───────────────────────────────────────────────────────────────
data class SongItemDto(
    val wordId: Int,
    val word: String,
    val description: String?,
    val videoUrl: String?,
    val sameMotionWord: List<String>
)

// ───────────────────────────────────────────────────────────────
// Retrofit 인터페이스
// ───────────────────────────────────────────────────────────────
interface StudyApiService {

    /** [1] 진행도 요약 */
    @GET("api/v1/study/progress")
    suspend fun getProgress(): Response<ProgressResponse>

    /** [2] 진행도 상세 */
    @GET("api/v1/study/progress/detail")
    suspend fun getProgressDetail(): Response<ProgressDetailResponse>

    /** [3] 일차 상세(단어/영상) */
    @GET("api/v1/study/days/{dayId}")
    suspend fun getDayDetail(
        @Path("dayId") dayId: Int
    ): Response<DayItemsResponse>

    /** [5] 노래학습 목록 (✔️ 새로 연결되는 곳) */
    @GET("api/v1/music/study")
    suspend fun getMusicStudyList(): Response<MusicStudyListResponse>

    /** [6] 노래학습 상세(단어/영상) */
    @GET("api/v1/study/music/{musicId}")
    suspend fun getSongStudy(
        @Path("musicId") musicId: Int
    ): Response<List<SongItemDto>>

    /** [4] 퀴즈 결과 저장 */
    @POST("api/v1/study/result")
    suspend fun postQuizResult(
        @Body body: QuizResultRequest
    ): Response<Unit>
}
