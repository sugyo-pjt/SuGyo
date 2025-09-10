package com.ssafy.a602.game.data

import java.time.LocalDateTime

/**
 * ERD의 users 테이블과 매핑되는 데이터 클래스
 */
data class User(
    val id: Long,                           // ERD: users.id (BIGINT)
    val email: String,                      // ERD: users.email (VARCHAR(100))
    val password: String,                   // ERD: users.password (VARCHAR(100))
    val createdAt: LocalDateTime,           // ERD: users.created_at (TIMESTAMP)
    val updatedAt: LocalDateTime,           // ERD: users.updated_at (TIMESTAMP)
    val nickname: String,                   // ERD: users.nickname (VARCHAR(10))
    val profileImageUrl: String? = null     // ERD: users.profile_image_url (VARCHAR(255))
)

/**
 * ERD의 rank 테이블과 매핑되는 데이터 클래스
 */
data class Rank(
    val id: Long,                           // ERD: rank.id (BIGINT)
    val musicId: Long,                      // ERD: rank.music_id (BIGINT)
    val userId: Long,                       // ERD: rank.user_id (BIGINT)
    val score: Int,                         // ERD: rank.score (INT)
    val recordTime: LocalDateTime           // ERD: rank.record_time (TIMESTAMP)
)
