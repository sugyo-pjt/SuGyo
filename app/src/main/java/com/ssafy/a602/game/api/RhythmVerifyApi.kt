package com.ssafy.a602.game.api

import com.ssafy.a602.game.play.dto.RhythmVerifyRequest
import com.ssafy.a602.game.play.dto.RhythmVerifyResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 리듬 검증 API
 */
interface RhythmVerifyApi {
    
    /**
     * 리듬 검증 요청
     * @param request 검증 요청 데이터
     * @return 검증 응답
     */
    @POST("rhythm/verify")
    suspend fun verifyRhythm(@Body request: RhythmVerifyRequest): Response<RhythmVerifyResponse>
}
