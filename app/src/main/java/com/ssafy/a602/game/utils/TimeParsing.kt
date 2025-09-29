package com.ssafy.a602.game.utils

/**
 * 시간 파싱 유틸리티 - 모든 시간 파싱을 통일
 * 다양한 시간 형식을 초 단위로 안전하게 변환
 */
object TimeParsing {
    /**
     * "HH:MM:SS", "HH:MM:SS.SSS", "MM:SS.SSS" 모두 허용. 실패 시 0f.
     */
    fun toSecondsOrZero(raw: String?): Float {
        if (raw.isNullOrBlank()) return 0f
        return try {
            val parts = raw.split(":")
            val (h, m, s) = when (parts.size) {
                3 -> Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toDouble())
                2 -> Triple(0, parts[0].toInt(), parts[1].toDouble())
                else -> Triple(0, 0, raw.toDouble())
            }
            (((h * 3600) + (m * 60)) * 1000 + (s * 1000)).toLong() / 1000f
        } catch (_: Exception) { 0f }
    }
}
