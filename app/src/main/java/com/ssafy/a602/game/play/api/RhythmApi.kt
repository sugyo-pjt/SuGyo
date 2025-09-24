package com.ssafy.a602.game.play.api

import com.ssafy.a602.game.play.dto.RhythmSaveRequest
import com.ssafy.a602.game.play.dto.SimilarityRequest
import com.ssafy.a602.game.play.dto.SimilarityResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * 하드 모드 리듬게임 API 인터페이스
 * 곡 종료 시 한 번에 업로드하는 API
 */
interface RhythmApi {
    
    /**
     * 리듬게임 데이터 저장 (곡 종료 시 한 번에 업로드)
     * 
     * @param request 리듬게임 데이터 요청
     * @param authorization Bearer 토큰
     * @return 성공 시 200, 실패 시 에러 응답
     */
    @POST("api/v1/game/rhythm/save")
    suspend fun saveRhythm(
        @Body request: RhythmSaveRequest,
        @Header("Authorization") authorization: String
    ): Response<RhythmSaveResponse>
    
    /**
     * 리듬게임 유사도 분석 (HTTP API)
     * 
     * @param request 유사도 분석 요청
     * @param authorization Bearer 토큰
     * @return 유사도 분석 결과
     */
    @POST("api/v1/game/rhythm/similarity")
    suspend fun getSimilarity(
        @Body request: SimilarityRequest,
        @Header("Authorization") authorization: String
    ): Response<SimilarityResponse>
}

/**
 * 리듬게임 데이터 저장 응답
 */
data class RhythmSaveResponse(
    val success: Boolean,
    val message: String? = null,
    val data: RhythmSaveData? = null
)

/**
 * 리듬게임 데이터 저장 결과 데이터
 */
data class RhythmSaveData(
    val musicId: Int,
    val totalFrames: Int,
    val totalSegments: Int,
    val savedAt: String
)
