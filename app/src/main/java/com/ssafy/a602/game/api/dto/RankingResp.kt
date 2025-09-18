package com.ssafy.a602.game.api.dto

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Json

/**
 * 랭킹 API 응답 DTO
 * GET /api/v1/game/rhythm/rank/{music_id}
 */
@JsonClass(generateAdapter = true)
data class RankingResp(
    val musicId: Long,
    val musicTitle: String,
    val ranking: List<RankingItemDto>,
    @Json(name = "myInfo") val myInfo: MyRankingInfoDto?
)

/**
 * 랭킹 아이템 DTO
 */
@JsonClass(generateAdapter = true)
data class RankingItemDto(
    val rank: Int,
    val userId: Long,
    val userNickName: String,
    val userProfileUrl: String,
    val score: Int,
    val recordDate: String
)

/**
 * 내 랭킹 정보 DTO
 */
@JsonClass(generateAdapter = true)
data class MyRankingInfoDto(
    val rank: Int,
    val score: Int,
    val recordDate: String
)
