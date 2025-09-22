package com.ssafy.a602.term.data.model

/**
 * 약관 요약 정보 DTO
 * API: GET /api/v1/term/summary
 */
data class TermSummaryDto(
    val id: Long,
    val mandatory: Boolean,
    val title: String
)
