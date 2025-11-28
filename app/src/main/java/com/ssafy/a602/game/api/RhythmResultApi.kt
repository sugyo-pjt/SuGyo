package com.ssafy.a602.game.api

import com.ssafy.a602.game.play.dto.RhythmResultRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * 리듬 게임 결과 전송 API
 */
interface RhythmResultApi {
    @POST("/api/v1/game/rhythm/result")
    suspend fun postResult(
        @Header("Authorization") bearer: String,
        @Body body: RhythmResultRequest
    ): Response<Unit>
}
