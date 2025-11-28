package com.ssafy.a602.game.api

import android.util.Log
import com.ssafy.a602.game.api.dto.ApiError
import retrofit2.HttpException
import java.io.IOException

/**
 * API 에러 처리 유틸리티 클래스
 * 공통 에러 포맷에 맞춰 에러를 처리하고 사용자 친화적인 메시지를 제공
 */
object ApiErrorHandler {
    
    private const val TAG = "ApiErrorHandler"
    
    /**
     * HTTP 에러를 처리하고 사용자 친화적인 메시지 반환
     */
    fun handleHttpError(exception: HttpException): String {
        return try {
            val errorBody = exception.response()?.errorBody()?.string()
            val apiError = parseApiError(errorBody)
            
            when (exception.code()) {
                401 -> "인증이 필요합니다. 다시 로그인해주세요."
                403 -> "접근 권한이 없습니다."
                404 -> apiError?.message ?: "요청한 리소스를 찾을 수 없습니다."
                500 -> "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
                else -> apiError?.message ?: "네트워크 오류가 발생했습니다. (${exception.code()})"
            }
        } catch (e: Exception) {
            Log.e(TAG, "에러 파싱 실패", e)
            "알 수 없는 오류가 발생했습니다."
        }
    }
    
    /**
     * 네트워크 에러를 처리하고 사용자 친화적인 메시지 반환
     */
    fun handleNetworkError(exception: IOException): String {
        return when {
            exception.message?.contains("timeout", ignoreCase = true) == true -> 
                "요청 시간이 초과되었습니다. 네트워크 연결을 확인해주세요."
            exception.message?.contains("connection", ignoreCase = true) == true -> 
                "네트워크 연결을 확인해주세요."
            else -> "네트워크 오류가 발생했습니다. 인터넷 연결을 확인해주세요."
        }
    }
    
    /**
     * 일반적인 예외를 처리하고 사용자 친화적인 메시지 반환
     */
    fun handleGenericError(exception: Exception): String {
        Log.e(TAG, "예상치 못한 오류 발생", exception)
        return "알 수 없는 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
    }
    
    /**
     * API 에러 응답을 파싱
     */
    private fun parseApiError(errorBody: String?): ApiError? {
        return try {
            if (errorBody.isNullOrEmpty()) return null
            
            // Gson을 사용하여 JSON 파싱
            val gson = com.google.gson.Gson()
            gson.fromJson(errorBody, ApiError::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "API 에러 파싱 실패: $errorBody", e)
            null
        }
    }
    
    /**
     * 에러 로깅
     */
    fun logError(tag: String, message: String, exception: Throwable? = null) {
        if (exception != null) {
            Log.e(tag, message, exception)
        } else {
            Log.e(tag, message)
        }
    }
}
