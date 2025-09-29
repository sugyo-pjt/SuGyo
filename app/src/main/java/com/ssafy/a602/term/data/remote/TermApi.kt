package com.ssafy.a602.term.data.remote

import com.ssafy.a602.term.data.model.TermDetailDto
import com.ssafy.a602.term.data.model.TermSummaryDto
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * 약관 관련 API 인터페이스
 * 
 * 스웨거 API 명세에 따른 엔드포인트:
 * - GET /api/v1/term/summary: 전체 약관 요약(title+id 목록)
 * - GET /api/v1/term/{id}: 약관 상세(id로 조회)
 */
interface TermApi {
    
    /**
     * 전체 약관 요약 조회
     * @return 약관 목록 (id, mandatory, title)
     */
    @GET("api/v1/term/summary")
    suspend fun getTermSummary(): List<TermSummaryDto>
    
    /**
     * 약관 상세 조회
     * @param id 약관 ID
     * @return 약관 상세 정보 (id, title, content, mandatory)
     */
    @GET("api/v1/term/{id}")
    suspend fun getTermDetail(@Path("id") id: Long): TermDetailDto
}
