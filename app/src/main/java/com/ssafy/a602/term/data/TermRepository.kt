package com.ssafy.a602.term.data

import com.ssafy.a602.term.data.model.TermDetailDto
import com.ssafy.a602.term.data.model.TermSummaryDto
import com.ssafy.a602.term.data.remote.TermApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 약관 관련 데이터 Repository
 * 
 * API 호출을 통한 약관 데이터 관리
 */
@Singleton
class TermRepository @Inject constructor(
    private val api: TermApi
) {
    
    /**
     * 약관 요약 목록 조회
     * @return 약관 요약 목록
     */
    suspend fun fetchSummaries(): Result<List<TermSummaryDto>> = runCatching {
        api.getTermSummary()
    }
    
    /**
     * 약관 상세 정보 조회
     * @param id 약관 ID
     * @return 약관 상세 정보
     */
    suspend fun fetchDetail(id: Long): Result<TermDetailDto> = runCatching {
        api.getTermDetail(id)
    }
}
