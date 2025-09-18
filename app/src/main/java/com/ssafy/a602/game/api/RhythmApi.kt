package com.ssafy.a602.game.api

import com.ssafy.a602.game.api.dto.ChartSegmentDto
import com.ssafy.a602.game.api.dto.CompleteReq
import com.ssafy.a602.game.api.dto.CompleteResp
import com.ssafy.a602.game.api.dto.MusicListItem
import com.ssafy.a602.game.api.dto.MusicUrl
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * 리듬 게임 API 인터페이스
 */
interface RhythmApi {
    
    /**
     * 노래 목록 조회
     * GET /api/v1/game/rhythm/music/list
     */
    @GET("/api/v1/game/rhythm/music/list")
    suspend fun getMusicList(): List<MusicListItem>
    
    /**
     * 노래(음원) 다운로드 URL 조회
     * GET /api/v1/game/rhythm/music/{music_id}
     */
    @GET("/api/v1/game/rhythm/music/{music_id}")
    suspend fun getMusicUrl(
        @Path("music_id") musicId: Long
    ): MusicUrl
    
    /**
     * 가사 및 채보 다운로드
     * GET /api/v1/game/rhythm/music/{music_id}/chart
     */
    @GET("/api/v1/game/rhythm/music/{music_id}/chart")
    suspend fun getChart(
        @Path("music_id") musicId: Long
    ): List<ChartSegmentDto>
    
    /**
     * 게임 완료 결과 전송
     * POST /api/v1/game/rhythm/complete
     */
    @POST("/api/v1/game/rhythm/complete")
    suspend fun complete(
        @Body body: CompleteReq
    ): CompleteResp
}
