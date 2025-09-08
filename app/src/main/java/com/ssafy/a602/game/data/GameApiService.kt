package com.ssafy.a602.game.data

import com.ssafy.a602.game.Song
import com.ssafy.a602.game.SongSection

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
                    duration = 4f,
                    lyrics = "어떤 길을 걸어도",
                    highlightRange = null
                ),
                SongSection(
                    startTime = 4f,
                    duration = 4f,
                    lyrics = "열린 문을 향해 나아가",
                    highlightRange = 0..2
                ),
                SongSection(
                    startTime = 8f,
                    duration = 4f,
                    lyrics = "우리가 함께 만들어가는",
                    highlightRange = null
                ),
                SongSection(
                    startTime = 12f,
                    duration = 4f,
                    lyrics = "새로운 세상",
                    highlightRange = null
                ),
                SongSection(
                    startTime = 16f,
                    duration = 4f,
                    lyrics = "함께 걸어가는 길",
                    highlightRange = 0..1
                ),
                SongSection(
                    startTime = 20f,
                    duration = 4f,
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
}
