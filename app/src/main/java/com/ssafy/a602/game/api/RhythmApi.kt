package com.ssafy.a602.game.api

import com.ssafy.a602.game.api.dto.ChartSegment
import com.ssafy.a602.game.api.dto.MusicListItem
import com.ssafy.a602.game.api.dto.MusicUrl
import retrofit2.http.GET
import retrofit2.http.Header
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
    suspend fun getMusicList(
        @Header("Authorization") bearer: String
    ): List<MusicListItem>
    
    /**
     * 노래(음원) 다운로드 URL 조회
     * GET /api/v1/game/rhythm/music/{music_id}
     */
    @GET("/api/v1/game/rhythm/music/{music_id}")
    suspend fun getMusicUrl(
        @Header("Authorization") bearer: String,
        @Path("music_id") musicId: Long
    ): MusicUrl
    
    /**
     * 가사 및 채보 다운로드
     * GET /api/v1/game/rhythm/music/{music_id}/chart
     */
    @GET("/api/v1/game/rhythm/music/{music_id}/chart")
    suspend fun getChart(
        @Header("Authorization") bearer: String,
        @Path("music_id") musicId: Long
    ): List<ChartSegment>
}
