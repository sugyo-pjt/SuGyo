package com.ssafy.a602.game.data

import com.ssafy.a602.game.result.GameResultUi
import com.ssafy.a602.game.songs.SongItem
import com.ssafy.a602.game.data.SongSection
import com.ssafy.a602.game.ranking.RankingItem
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
        missWords: List<String>
    ): GameResultUi
}

/**
 * 더미 API 서비스 구현체
 * TODO: 실제 API 연동 시 이 클래스를 실제 API 호출로 교체
 */
class DummyGameApiService : GameApiService {
    
    override suspend fun getSongs(): List<SongItem> {
        // 실제로는 네트워크 호출
        return FakeSongs.items
    }
    
    override suspend fun searchSongs(query: String): List<SongItem> {
        // 실제로는 서버에서 검색
        val songs = getSongs()
        return if (query.isBlank()) {
            songs
        } else {
            songs.filter { song ->
                song.title.contains(query, ignoreCase = true) ||
                song.artist.contains(query, ignoreCase = true)
            }
        }
    }
    
    override suspend fun getSongSections(songId: String): List<SongSection> {
        // 실제로는 서버에서 소절 정보 가져오기
        return when (songId) {
            "way_back_home" -> listOf(
                SongSection(
                    id = "1",
                    songId = songId,
                    startTime = 0f,
                    endTime = 10f,
                    text = "어떤 길을 걸어도"
                ),
                SongSection(
                    id = "2",
                    songId = songId,
                    startTime = 10f,
                    endTime = 20f,
                    text = "열린 문을 향해 나아가"
                ),
                SongSection(
                    id = "3",
                    songId = songId,
                    startTime = 20f,
                    endTime = 30f,
                    text = "우리가 함께 만들어가는"
                ),
                SongSection(
                    id = "4",
                    songId = songId,
                    startTime = 30f,
                    endTime = 40f,
                    text = "새로운 세상"
                ),
                SongSection(
                    id = "5",
                    songId = songId,
                    startTime = 40f,
                    endTime = 50f,
                    text = "함께 걸어가는 길"
                ),
                SongSection(
                    id = "6",
                    songId = songId,
                    startTime = 50f,
                    endTime = 62f,
                    text = "언제나 너와 함께"
                )
            )
            "asap" -> listOf(
                SongSection(
                    id = "7",
                    songId = songId,
                    startTime = 0f,
                    endTime = 3f,
                    text = "ASAP"
                ),
                SongSection(
                    id = "8",
                    songId = songId,
                    startTime = 3f,
                    endTime = 6f,
                    text = "STAYC girls"
                ),
                SongSection(
                    id = "9",
                    songId = songId,
                    startTime = 6f,
                    endTime = 9f,
                    text = "It's going down"
                ),
                SongSection(
                    id = "10",
                    songId = songId,
                    startTime = 9f,
                    endTime = 12f,
                    text = "We're going up"
                )
            )
            else -> emptyList()
        }
    }
    
    override suspend fun getMusicUrl(songId: String): String {
        // 실제로는 서버에서 음악 URL 가져오기
        return when (songId) {
            "way_back_home" -> "https://example.com/music/way_back_home.mp3"
            "asap" -> "https://example.com/music/asap.mp3"
            else -> "https://example.com/music/default.mp3"
        }
    }
    
    override suspend fun saveGameResult(result: GameResultUi): Boolean {
        // 실제로는 서버에 결과 저장
        return true
    }
    
    override suspend fun getRankings(songId: String): List<RankingItem> {
        // 실제로는 서버에서 순위 가져오기
        return listOf(
            RankingItem(
                rank = 1,
                nickname = "플레이어1",
                score = 95000,
                playedDate = LocalDate.now()
            ),
            RankingItem(
                rank = 2,
                nickname = "플레이어2",
                score = 92000,
                playedDate = LocalDate.now()
            ),
            RankingItem(
                rank = 3,
                nickname = "플레이어3",
                score = 89000,
                playedDate = LocalDate.now()
            )
        )
    }
    
    override suspend fun getTop3Rankings(songId: String): List<RankingItem> {
        val allRankings = getRankings(songId)
        return allRankings.take(3)
    }
    
    override suspend fun submitRanking(songId: String, score: Int, nickname: String): RankingItem {
        // 실제로는 서버에 순위 제출
        val song = getSongs().find { it.id == songId }
        return RankingItem(
            rank = 1, // 서버에서 계산된 순위
            nickname = nickname,
            score = score,
            playedDate = LocalDate.now()
        )
    }
    
    override suspend fun getUserBestScore(songId: String): Int? {
        // 실제로는 서버에서 사용자 최고 점수 가져오기
        val song = getSongs().find { it.id == songId }
        return song?.bestScore
    }
    
    override suspend fun getMyRanking(songId: String): RankingItem? {
        // 실제로는 서버에서 내 순위 가져오기
        return null
    }
    
    override suspend fun calculateGameResult(
        songId: String,
        score: Int,
        correctCount: Int,
        missCount: Int,
        maxCombo: Int,
        missWords: List<String>
    ): GameResultUi {
        // 실제로는 서버에서 게임 결과 계산
        val song = getSongs().find { it.id == songId }
        val accuracyPercent = if (correctCount + missCount > 0) (correctCount * 100 / (correctCount + missCount)) else 0
        
        return GameResultUi(
            songTitle = song?.title ?: "Unknown Song",
            score = score,
            accuracyPercent = accuracyPercent,
            grade = when {
                accuracyPercent >= 95 -> "S"
                accuracyPercent >= 85 -> "A"
                accuracyPercent >= 70 -> "B"
                accuracyPercent >= 50 -> "C"
                else -> "F"
            },
            maxCombo = maxCombo,
            correctCount = correctCount,
            missCount = missCount,
            comboMultiplier = when {
                maxCombo >= 50 -> 1.5
                maxCombo >= 30 -> 1.3
                maxCombo >= 20 -> 1.2
                maxCombo >= 10 -> 1.1
                else -> 1.0
            },
            isNewRecord = false,
            missWords = missWords
        )
    }
}