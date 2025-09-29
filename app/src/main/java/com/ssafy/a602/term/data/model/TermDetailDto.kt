package com.ssafy.a602.term.data.model

/**
 * 약관 상세 정보 DTO
 * API: GET /api/v1/term/{id}
 */
data class TermDetailDto(
    val id: Long,
    val title: String,
    val content: String,
    val mandatory: Boolean
)
