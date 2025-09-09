package com.ssafy.a602.game.data

import com.ssafy.a602.game.Song
import com.ssafy.a602.game.SongSection
import com.ssafy.a602.game.RankingItem
import java.time.LocalDate

/**
 * 게임 API 서비스 인터페이스
 * 실제 API 연동 시 이 인터페이스를 구현하면 됨
 */
interface GameApiService {
    
    /**
     * 곡 목록 가져오기
     */
    suspend fun getSongs(): List<Song>
    
    /**
     * 곡 검색
     */
    suspend fun searchSongs(query: String): List<Song>
    
    /**
     * 특정 곡의 소절 정보 가져오기
     */
    suspend fun getSongSections(songId: String): List<SongSection>
    
    /**
     * 게임 결과 저장
     */
    suspend fun saveGameResult(songId: String, score: Int, accuracy: Float)
    
    /**
     * 사용자의 최고 점수 가져오기
     */
    suspend fun getUserBestScore(songId: String): Int?
    
    /**
     * 특정 곡의 순위 목록 가져오기
     */
    suspend fun getRankings(songId: String): List<RankingItem>
    
    /**
     * 특정 곡의 Top 3 순위 가져오기
     */
    suspend fun getTop3Rankings(songId: String): List<RankingItem>
    
    /**
     * 내 순위 가져오기
     */
    suspend fun getMyRanking(songId: String): RankingItem?
}

/**
 * 더미 API 서비스 구현체
 * TODO: 실제 API 연동 시 이 클래스를 실제 API 호출로 교체
 */
class DummyGameApiService : GameApiService {
    
    override suspend fun getSongs(): List<Song> {
        // 실제로는 네트워크 호출
        return FakeSongs.items
    }
    
    override suspend fun searchSongs(query: String): List<Song> {
        // 실제로는 서버에서 검색
        val songs = getSongs()
        return if (query.isBlank()) {
            songs
        } else {
            val q = query.trim().lowercase()
            songs.filter { 
                it.title.lowercase().contains(q) || 
                it.artist.lowercase().contains(q) 
            }
        }
    }
    
    override suspend fun getSongSections(songId: String): List<SongSection> {
        // 실제로는 서버에서 소절 정보 가져오기
        return when (songId) {
            "way_back_home" -> listOf(
                SongSection(
                    startTime = 0f,
                    duration = 10f,
                    lyrics = "어떤 길을 걸어도",
                    highlightRange = null
                ),
                SongSection(
                    startTime = 10f,
                    duration = 10f,
                    lyrics = "열린 문을 향해 나아가",
                    highlightRange = 0..2
                ),
                SongSection(
                    startTime = 20f,
                    duration = 10f,
                    lyrics = "우리가 함께 만들어가는",
                    highlightRange = null
                ),
                SongSection(
                    startTime = 30f,
                    duration = 10f,
                    lyrics = "새로운 세상",
                    highlightRange = null
                ),
                SongSection(
                    startTime = 40f,
                    duration = 10f,
                    lyrics = "함께 걸어가는 길",
                    highlightRange = 0..1
                ),
                SongSection(
                    startTime = 50f,
                    duration = 12f,
                    lyrics = "언제나 너와 함께",
                    highlightRange = null
                )
            )
            "asap" -> listOf(
                SongSection(
                    startTime = 0f,
                    duration = 3f,
                    lyrics = "ASAP",
                    highlightRange = null
                ),
                SongSection(
                    startTime = 3f,
                    duration = 3f,
                    lyrics = "STAYC girls",
                    highlightRange = null
                ),
                SongSection(
                    startTime = 6f,
                    duration = 3f,
                    lyrics = "It's going down",
                    highlightRange = 0..1
                ),
                SongSection(
                    startTime = 9f,
                    duration = 3f,
                    lyrics = "We're going up",
                    highlightRange = null
                ),
                SongSection(
                    startTime = 12f,
                    duration = 3f,
                    lyrics = "ASAP ASAP",
                    highlightRange = null
                )
            )
            "hello" -> listOf(
                SongSection(
                    startTime = 0f,
                    duration = 3f,
                    lyrics = "안녕하세요",
                    highlightRange = null
                ),
                SongSection(
                    startTime = 3f,
                    duration = 3f,
                    lyrics = "처음 뵙겠습니다",
                    highlightRange = null
                )
            )
            else -> listOf(
                SongSection(
                    startTime = 0f,
                    duration = 10f,
                    lyrics = "기본 가사",
                    highlightRange = null
                )
            )
        }
    }
    
    override suspend fun saveGameResult(songId: String, score: Int, accuracy: Float) {
        // 실제로는 서버에 결과 저장
        // TODO: API 호출 구현
    }
    
    override suspend fun getUserBestScore(songId: String): Int? {
        // 실제로는 서버에서 사용자 최고 점수 가져오기
        // TODO: API 호출 구현
        return null
    }
    
    override suspend fun getRankings(songId: String): List<RankingItem> {
        // 실제로는 서버에서 순위 목록 가져오기
        // TODO: API 호출 구현
        return getDummyRankings(songId)
    }
    
    override suspend fun getTop3Rankings(songId: String): List<RankingItem> {
        // 실제로는 서버에서 Top 3 순위 가져오기
        // TODO: API 호출 구현
        return getDummyRankings(songId).take(3)
    }
    
    override suspend fun getMyRanking(songId: String): RankingItem? {
        // 실제로는 서버에서 내 순위 가져오기
        // TODO: API 호출 구현
        return getDummyMyRanking(songId)
    }
    
    /**
     * 더미 순위 데이터 생성
     */
    private fun getDummyRankings(songId: String): List<RankingItem> {
        val baseDate = LocalDate.now().minusDays(30)
        return when (songId) {
            "way_back_home" -> listOf(
                RankingItem(1, "수어마스터", 987650, baseDate.minusDays(2)),
                RankingItem(2, "리듬킹", 965420, baseDate.minusDays(5)),
                RankingItem(3, "사인랭커", 944300, baseDate.minusDays(1)),
                RankingItem(4, "수어고수", 923150, baseDate.minusDays(7)),
                RankingItem(5, "손짓왕", 901200, baseDate.minusDays(3)),
                RankingItem(6, "수어신", 889750, baseDate.minusDays(10)),
                RankingItem(7, "제스처마스터", 876300, baseDate.minusDays(4)),
                RankingItem(8, "수어전사", 864200, baseDate.minusDays(8)),
                RankingItem(9, "손동작킹", 852100, baseDate.minusDays(6)),
                RankingItem(10, "수어마법사", 840000, baseDate.minusDays(9))
            )
            "asap" -> listOf(
                RankingItem(1, "STAYC팬", 876500, baseDate.minusDays(1)),
                RankingItem(2, "ASAP러버", 854200, baseDate.minusDays(3)),
                RankingItem(3, "K팝킹", 832100, baseDate.minusDays(2)),
                RankingItem(4, "음악마스터", 810000, baseDate.minusDays(5)),
                RankingItem(5, "리듬킹", 788500, baseDate.minusDays(4))
            )
            "hello" -> listOf(
                RankingItem(1, "인사왕", 952000, baseDate.minusDays(1)),
                RankingItem(2, "안녕마스터", 934500, baseDate.minusDays(2)),
                RankingItem(3, "기초킹", 917200, baseDate.minusDays(3)),
                RankingItem(4, "수어초보", 900000, baseDate.minusDays(4)),
                RankingItem(5, "학습자", 882800, baseDate.minusDays(5))
            )
            else -> listOf(
                RankingItem(1, "플레이어1", 800000, baseDate.minusDays(1)),
                RankingItem(2, "플레이어2", 750000, baseDate.minusDays(2)),
                RankingItem(3, "플레이어3", 700000, baseDate.minusDays(3))
            )
        }
    }
    
    /**
     * 더미 내 순위 데이터 생성
     */
    private fun getDummyMyRanking(songId: String): RankingItem? {
        val baseDate = LocalDate.now().minusDays(15)
        return when (songId) {
            "way_back_home" -> RankingItem(15, "나", 756800, baseDate, isMe = true)
            "asap" -> RankingItem(8, "나", 723400, baseDate, isMe = true)
            "hello" -> RankingItem(12, "나", 845600, baseDate, isMe = true)
            else -> RankingItem(25, "나", 650000, baseDate, isMe = true)
        }
    }
}
