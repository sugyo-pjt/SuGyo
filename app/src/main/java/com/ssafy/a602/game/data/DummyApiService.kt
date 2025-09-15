package com.ssafy.a602.game.data

import android.util.Log
import com.ssafy.a602.game.api.dto.ChartCorrect
import com.ssafy.a602.game.songs.SongItem
import com.ssafy.a602.game.result.GameResultUi
import com.ssafy.a602.game.ranking.RankingItem
import com.ssafy.a602.game.score.GameResultRequest
import kotlinx.coroutines.delay
import java.time.LocalDate

/**
 * 더미 데이터를 제공하는 API 서비스
 * 미디어파이프 좌표 데이터 테스트를 위한 샘플 곡들과 가사 데이터
 */
class DummyApiService : GameApiService {
    
    override suspend fun getSongs(): List<SongItem> {
        delay(100) // 네트워크 지연 시뮬레이션
        Log.d("DummyApiService", "더미 곡 목록 반환")
        
        return listOf(
            SongItem(
                id = "dummy_song_1",
                title = "테스트 곡 1 - 안녕하세요",
                artist = "테스트 아티스트",
                durationText = "2:30",
                bestScore = null,
                albumImageUrl = null
            ),
            SongItem(
                id = "dummy_song_2", 
                title = "테스트 곡 2 - 사과와 바나나",
                artist = "테스트 아티스트",
                durationText = "3:15",
                bestScore = 85000,
                albumImageUrl = null
            ),
            SongItem(
                id = "dummy_song_3",
                title = "테스트 곡 3 - 좋은 하루",
                artist = "테스트 아티스트", 
                durationText = "2:45",
                bestScore = 92000,
                albumImageUrl = null
            )
        )
    }
    
    override suspend fun searchSongs(query: String): List<SongItem> {
        delay(50)
        val allSongs = getSongs()
        return allSongs.filter { song ->
            song.title.contains(query, ignoreCase = true) ||
            song.artist.contains(query, ignoreCase = true)
        }
    }
    
    override suspend fun getSongSections(songId: String): List<SongSection> {
        delay(150) // 네트워크 지연 시뮬레이션
        Log.d("DummyApiService", "더미 가사 데이터 반환: songId=$songId")
        
        return when (songId) {
            "dummy_song_1" -> createDummySong1Sections()
            "dummy_song_2" -> createDummySong2Sections()
            "dummy_song_3" -> createDummySong3Sections()
            else -> emptyList()
        }
    }
    
    private fun createDummySong1Sections(): List<SongSection> {
        return listOf(
            SongSection(
                id = "1",
                songId = "dummy_song_1",
                startTime = 0f,
                endTime = 3f,
                text = "안녕하세요",
                correctInfo = listOf(
                    ChartCorrect(
                        correctStartedIndex = 0,
                        correctEndedIndex = 5,
                        actionStartedAt = "00:00:00.50",
                        actionEndedAt = "00:00:02.50"
                    )
                )
            ),
            SongSection(
                id = "2", 
                songId = "dummy_song_1",
                startTime = 3f,
                endTime = 6f,
                text = "반갑습니다",
                correctInfo = listOf(
                    ChartCorrect(
                        correctStartedIndex = 0,
                        correctEndedIndex = 5,
                        actionStartedAt = "00:00:03.50",
                        actionEndedAt = "00:00:05.50"
                    )
                )
            ),
            SongSection(
                id = "3",
                songId = "dummy_song_1", 
                startTime = 6f,
                endTime = 9f,
                text = "오늘도 좋은 하루",
                correctInfo = listOf(
                    ChartCorrect(
                        correctStartedIndex = 0,
                        correctEndedIndex = 8,
                        actionStartedAt = "00:00:06.50",
                        actionEndedAt = "00:00:08.50"
                    )
                )
            ),
            SongSection(
                id = "4",
                songId = "dummy_song_1",
                startTime = 9f,
                endTime = 12f,
                text = "감사합니다",
                correctInfo = listOf(
                    ChartCorrect(
                        correctStartedIndex = 0,
                        correctEndedIndex = 5,
                        actionStartedAt = "00:00:09.50",
                        actionEndedAt = "00:00:11.50"
                    )
                )
            )
        )
    }
    
    private fun createDummySong2Sections(): List<SongSection> {
        return listOf(
            SongSection(
                id = "1",
                songId = "dummy_song_2",
                startTime = 0f,
                endTime = 4f,
                text = "사과는 빨간색",
                correctInfo = listOf(
                    ChartCorrect(
                        correctStartedIndex = 0,
                        correctEndedIndex = 6,
                        actionStartedAt = "00:00:00.50",
                        actionEndedAt = "00:00:03.50"
                    )
                )
            ),
            SongSection(
                id = "2",
                songId = "dummy_song_2", 
                startTime = 4f,
                endTime = 8f,
                text = "바나나는 노란색",
                correctInfo = listOf(
                    ChartCorrect(
                        correctStartedIndex = 0,
                        correctEndedIndex = 7,
                        actionStartedAt = "00:00:04.50",
                        actionEndedAt = "00:00:07.50"
                    )
                )
            ),
            SongSection(
                id = "3",
                songId = "dummy_song_2",
                startTime = 8f,
                endTime = 12f,
                text = "둘 다 맛있어요",
                correctInfo = listOf(
                    ChartCorrect(
                        correctStartedIndex = 0,
                        correctEndedIndex = 7,
                        actionStartedAt = "00:00:08.50",
                        actionEndedAt = "00:00:11.50"
                    )
                )
            )
        )
    }
    
    private fun createDummySong3Sections(): List<SongSection> {
        return listOf(
            SongSection(
                id = "1",
                songId = "dummy_song_3",
                startTime = 0f,
                endTime = 3f,
                text = "좋은 아침",
                correctInfo = listOf(
                    ChartCorrect(
                        correctStartedIndex = 0,
                        correctEndedIndex = 4,
                        actionStartedAt = "00:00:00.50",
                        actionEndedAt = "00:00:02.50"
                    )
                )
            ),
            SongSection(
                id = "2",
                songId = "dummy_song_3",
                startTime = 3f,
                endTime = 6f,
                text = "햇살이 따뜻해",
                correctInfo = listOf(
                    ChartCorrect(
                        correctStartedIndex = 0,
                        correctEndedIndex = 6,
                        actionStartedAt = "00:00:03.50",
                        actionEndedAt = "00:00:05.50"
                    )
                )
            ),
            SongSection(
                id = "3",
                songId = "dummy_song_3",
                startTime = 6f,
                endTime = 9f,
                text = "오늘도 화이팅",
                correctInfo = listOf(
                    ChartCorrect(
                        correctStartedIndex = 0,
                        correctEndedIndex = 6,
                        actionStartedAt = "00:00:06.50",
                        actionEndedAt = "00:00:08.50"
                    )
                )
            )
        )
    }
    
    override suspend fun getMusicUrl(songId: String): String {
        delay(100)
        // 더미 오디오 파일 URL (실제로는 로컬 리소스나 테스트용 오디오 사용)
        return "https://www.soundjay.com/misc/sounds/bell-ringing-05.wav"
    }
    
    override suspend fun calculateGameResult(
        songId: String,
        score: Int,
        correctCount: Int,
        missCount: Int,
        maxCombo: Int,
        missWords: List<String>
    ): GameResultUi {
        delay(200)
        val accuracyPercent = if (correctCount + missCount > 0) {
            ((correctCount.toFloat() / (correctCount + missCount)) * 100).toInt()
        } else 0
        
        return GameResultUi(
            songTitle = "테스트 곡",
            score = score,
            accuracyPercent = accuracyPercent,
            grade = when {
                score >= 90000 -> "S"
                score >= 80000 -> "A"
                score >= 70000 -> "B"
                score >= 60000 -> "C"
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
            isNewRecord = true,
            missWords = missWords
        )
    }
    
    override suspend fun saveGameResult(result: GameResultUi): Boolean {
        delay(100)
        Log.d("DummyApiService", "게임 결과 저장: ${result.songTitle}, 점수: ${result.score}")
        return true
    }
    
    override suspend fun getUserBestScore(songId: String): Int? {
        delay(50)
        return null // 더미에서는 항상 null 반환 (기록 없음)
    }
    
    override suspend fun getRankings(songId: String): List<RankingItem> {
        delay(100)
        return emptyList() // 더미에서는 빈 리스트 반환
    }
    
    override suspend fun getTop3Rankings(songId: String): List<RankingItem> {
        delay(100)
        return emptyList()
    }
    
    override suspend fun submitRanking(songId: String, score: Int, nickname: String): RankingItem {
        delay(100)
        return RankingItem(
            rank = 1,
            nickname = nickname,
            score = score,
            playedDate = LocalDate.now(),
            isMe = true
        )
    }
    
    override suspend fun getMyRanking(songId: String): RankingItem? {
        delay(50)
        return null
    }
    
    override suspend fun submitGameResult(result: GameResultRequest): GameResultUi {
        delay(200)
        Log.d("DummyApiService", "게임 결과 전송: ${result.songId}, 점수: ${result.totalScore}")
        
        return GameResultUi(
            songTitle = "테스트 곡",
            score = result.totalScore,
            accuracyPercent = result.percent,
            grade = result.grade,
            maxCombo = result.maxCombo,
            correctCount = result.correctCount,
            missCount = result.missCount,
            comboMultiplier = when {
                result.maxCombo >= 50 -> 1.5
                result.maxCombo >= 30 -> 1.3
                result.maxCombo >= 20 -> 1.2
                result.maxCombo >= 10 -> 1.1
                else -> 1.0
            },
            isNewRecord = true, // 더미에서는 항상 신기록으로 설정
            missWords = result.missWords,
            accepted = true,
            isPersonalBest = true, // 더미에서는 항상 개인 최고 기록으로 설정
            rankUpdated = true, // 더미에서는 항상 순위 업데이트로 설정
            serverScoreEcho = result.totalScore
        )
    }
}
