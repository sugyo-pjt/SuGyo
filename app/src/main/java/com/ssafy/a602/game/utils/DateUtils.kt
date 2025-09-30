package com.ssafy.a602.game.utils

import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 게임 관련 날짜/시간 유틸리티
 */
object DateUtils {
    
    private val ISO_LDT = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val KST = ZoneId.of("Asia/Seoul")
    private val UI_FMT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.KOREA)
    private val DATE_ONLY_FMT = DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREA)
    
    /**
     * ISO LocalDateTime 문자열을 안전하게 파싱하여 UI용 텍스트로 변환
     * @param iso ISO LocalDateTime 형식 문자열 (예: "2025-09-18T21:05:31")
     * @return UI용 포맷된 문자열 (예: "2025.09.18 21:05") 또는 "-" (파싱 실패 시)
     */
    fun parseRecordDateToUiText(iso: String?): String {
        if (iso.isNullOrBlank()) return "-"
        return try {
            val ldt = LocalDateTime.parse(iso, ISO_LDT)
            // 서버 타임존이 명시 없으므로 '로컬' 기준으로 보고 KST 표기로만 맞춤
            ldt.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(KST)
                .format(UI_FMT)
        } catch (_: Exception) {
            "-" // 파싱 실패 시 안전하게 대체
        }
    }
    
    /**
     * ISO LocalDateTime 문자열을 안전하게 LocalDate로 변환
     * @param iso ISO LocalDateTime 형식 문자열 (예: "2025-09-18T21:05:31")
     * @return LocalDate 또는 null (파싱 실패 시)
     */
    fun parseRecordDateToLocalDate(iso: String?): LocalDate? {
        if (iso.isNullOrBlank()) return null
        return try {
            val ldt = LocalDateTime.parse(iso, ISO_LDT)
            ldt.toLocalDate()
        } catch (_: Exception) {
            null // 파싱 실패 시 null 반환
        }
    }
    
    /**
     * ISO LocalDateTime 문자열을 안전하게 LocalDateTime으로 변환
     * @param iso ISO LocalDateTime 형식 문자열 (예: "2025-09-18T21:05:31")
     * @return LocalDateTime 또는 null (파싱 실패 시)
     */
    fun parseRecordDateToLocalDateTime(iso: String?): LocalDateTime? {
        if (iso.isNullOrBlank()) return null
        return try {
            LocalDateTime.parse(iso, ISO_LDT)
        } catch (_: Exception) {
            null // 파싱 실패 시 null 반환
        }
    }
    
    /**
     * LocalDate를 UI용 날짜 문자열로 변환
     * @param date LocalDate
     * @return UI용 포맷된 문자열 (예: "2025.09.18")
     */
    fun formatLocalDateToUi(date: LocalDate?): String {
        return date?.format(DATE_ONLY_FMT) ?: "-"
    }
}
