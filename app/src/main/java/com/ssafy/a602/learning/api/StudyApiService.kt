package com.ssafy.a602.learning.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

// ───────────────────────────────────────────────────────────────
// [1] 메인 화면: 진행도 요약 (progressDay만 필요)
//    사용처: LearningMainPage (LearningViewModel)
//    응답 예: {"progressDay": 2}
// ───────────────────────────────────────────────────────────────
data class ProgressResponse(
    val progressDay: Int
)

// ───────────────────────────────────────────────────────────────
// [2] 로드맵 화면: 진행도 상세 (totalDays + progressDay + days[])
//    사용처: Total_RoadMap (RoadmapViewModel)
//    Swagger 예:
//    {
//      "totalDays": 3,
//      "progressDay": 2,
//      "days": [
//        {"dayId":1,"day":1,"correctCount":6,"totalCount":6}, ...
//      ]
//    }
// ───────────────────────────────────────────────────────────────
data class ProgressDetailResponse(
    val totalDays: Int,
    val progressDay: Int,
    val days: List<DayDetail> = emptyList()
)

/** 일차별 통계 한 줄(로드맵 카드 계산용) */
data class DayDetail(
    val dayId: Long? = null,     // 서버에서 내려오면 사용, 아니면 null
    val day: Int,                // Day 번호 (1,2,3…)
    val correctCount: Int? = null,
    val totalCount: Int
)

// ───────────────────────────────────────────────────────────────
// [3] 일차 상세: 단어/영상 목록
//    사용처: DailyDetailStudyScreen (DailyDetailStudyViewModel)
//    Swagger 예:
//    {
//      "day": 1,
//      "items": [
//        {"wordId":1444,"word":"안녕하세요","description":"...","videoUrl":"https://..."},
//        {"wordId":1633,"word":"나","description":"...","videoUrl":"https://..."}
//      ]
//    }
// ───────────────────────────────────────────────────────────────
data class DayItemsResponse(
    val day: Int,
    val items: List<DayItemDto>
)


/** 일차 상세의 개별 아이템(단어/영상) */
data class DayItemDto(
    val wordId: Long,
    val word: String,
    val description: String?,   // UI에서 안 쓸 수도 있어 nullable
    val videoUrl: String?       // 영상이 없을 수도 있어 nullable
)

data class SongItemDto(
    val wordId: Int,
    val word: String,
    val description: String?,
    val videoUrl: String?,
    val sameMotionWord: List<String>
)

// ───────────────────────────────────────────────────────────────
// Retrofit 인터페이스
//  - BASE_URL 뒤에 그대로 붙습니다(앞에 슬래시 X)
//  - Authorization은 AuthInterceptor가 자동 부착
// ───────────────────────────────────────────────────────────────
interface StudyApiService {

    /** [1] 진행도 요약: LearningMainPage */
    @GET("api/v1/study/progress")
    suspend fun getProgress(): Response<ProgressResponse>

    /** [2] 진행도 상세: Total_RoadMap */
    @GET("api/v1/study/progress/detail")
    suspend fun getProgressDetail(): Response<ProgressDetailResponse>

    /** [3] 일차 상세(단어/영상): DailyDetailStudyScreen */
    @GET("api/v1/study/days/{dayId}")
    suspend fun getDayDetail(
        @Path("dayId") dayId: Int
    ): Response<DayItemsResponse>

    /** [4] 노래학습 상세(단어/영상): SongStudyScreen */
    @GET("api/v1/study/music/{musicId}")
    suspend fun getSongStudy(
        @Path("musicId") musicId: Int
    ): Response<List<SongItemDto>>
}
