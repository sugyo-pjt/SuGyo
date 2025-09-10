package com.ssafy.a602.game.data

/**
 * ERD의 vocabulary 테이블과 매핑되는 데이터 클래스
 */
data class Vocabulary(
    val id: Long,                           // ERD: vocabulary.id (BIGINT)
    val word: String,                       // ERD: vocabulary.word (VARCHAR(50))
    val description: String,                // ERD: vocabulary.description (TEXT)
    val videoUrl: String                    // ERD: vocabulary.video_url (VARCHAR(255))
)

/**
 * ERD의 daily 테이블과 매핑되는 데이터 클래스
 */
data class Daily(
    val id: Long,                           // ERD: daily.id (BIGINT)
    val day: Int,                           // ERD: daily.day (INT)
    val sentence: String? = null            // ERD: daily.sentence (VARCHAR(255))
)

/**
 * ERD의 music_vocabulary 테이블과 매핑되는 데이터 클래스 (Junction Table)
 */
data class MusicVocabulary(
    val id: Long,                           // ERD: music_vocabulary.id (BIGINT)
    val musicId: Long,                      // ERD: music_vocabulary.music_id (BIGINT)
    val vocabularyId: Long                  // ERD: music_vocabulary.vocabulary_id (BIGINT)
)

/**
 * ERD의 daily_vocabulary 테이블과 매핑되는 데이터 클래스 (Junction Table)
 */
data class DailyVocabulary(
    val id: Long,                           // ERD: daily_vocabulary.id (BIGINT)
    val dailyId: Long,                      // ERD: daily_vocabulary.daily_id (BIGINT)
    val vocabularyId: Long                  // ERD: daily_vocabulary.vocabulary_id (BIGINT)
)

/**
 * ERD의 user_daily_vocabulary 테이블과 매핑되는 데이터 클래스
 */
data class UserDailyVocabulary(
    val id: Long,                           // ERD: user_daily_vocabulary.id (BIGINT)
    val userId: Long,                       // ERD: user_daily_vocabulary.user_id (BIGINT)
    val dailyId: Long,                      // ERD: user_daily_vocabulary.daily_id (BIGINT)
    val completed: Boolean = false,         // ERD: user_daily_vocabulary.completed (BOOLEAN, Default: false)
    val quizScore: Int? = null              // ERD: user_daily_vocabulary.quiz_score (INT)
)
