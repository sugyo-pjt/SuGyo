package com.ssafy.a602.learning.api

import retrofit2.Response
import retrofit2.http.GET

// 서버 응답 DTO (Postman: {"progressDay": 2})
data class ProgressResponse(val progressDay: Int)

// 기존 진행도 응답 --------------------------------
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

interface StudyApiService {
    // BASE_URL 뒤에 그대로 붙습니다. (선행 슬래시 X)
    @GET("api/v1/study/progress")
    suspend fun getProgress(): Response<ProgressResponse>

    // 신규: 상세 진행도 (totalDays / progressDay 포함)
    @GET("api/v1/study/progress/detail")
    suspend fun getProgressDetail(): Response<ProgressDetailResponse>
}
