package com.ssafy.a602.game.data

import com.ssafy.a602.game.result.GameResultUi
import com.ssafy.a602.game.songs.SongItem
import com.ssafy.a602.game.data.SongSection
import com.ssafy.a602.game.ranking.RankingItem
import com.ssafy.a602.game.score.GameResultRequest
import java.time.LocalDate

/**
 * 게임 API 서비스 인터페이스
 * ERD 구조에 맞춰 설계됨
 * 실제 API 연동 시 이 인터페이스를 구현하면 됨
 */
interface GameApiService {
    
    /**
     * 곡 목록 가져오기
     */
    suspend fun getSongs(): List<SongItem>
    
    /**
     * 곡 검색
     */
    suspend fun searchSongs(query: String): List<SongItem>
    
    /**
     * 특정 곡의 소절 정보 가져오기
     */
    suspend fun getSongSections(songId: String): List<SongSection>
    
    /**
     * 음악 URL 가져오기 (ExoPlayer용)
     */
    suspend fun getMusicUrl(songId: String): String
    
    /**
     * 게임 결과 저장
     */
    suspend fun saveGameResult(result: GameResultUi): Boolean
    
    /**
     * 특정 곡의 순위 가져오기
     */
    suspend fun getRankings(songId: String): List<RankingItem>
    
    /**
     * 특정 곡의 상위 3명 순위 가져오기
     */
    suspend fun getTop3Rankings(songId: String): List<RankingItem>
    
    /**
     * 사용자 순위 등록
     */
    suspend fun submitRanking(songId: String, score: Int, nickname: String): RankingItem
    
    /**
     * 사용자 최고 점수 가져오기
     */
    suspend fun getUserBestScore(songId: String): Int?
    
    /**
     * 내 순위 가져오기
     */
    suspend fun getMyRanking(songId: String): RankingItem?
    
    /**
     * 게임 결과 계산
     */
    suspend fun calculateGameResult(
        songId: String,
        score: Int,
        correctCount: Int,
        missCount: Int,
        maxCombo: Int,
        missWords: List<String>,
        perfectCount: Int = 0,
        goodCount: Int = 0,
        totalJudgments: Int = 0
    ): GameResultUi
    
    /**
     * 게임 결과 전송 (프론트에서 계산된 결과)
     */
    suspend fun submitGameResult(result: GameResultRequest): GameResultUi
}
