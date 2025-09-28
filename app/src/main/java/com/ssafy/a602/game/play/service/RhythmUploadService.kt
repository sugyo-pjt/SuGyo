package com.ssafy.a602.game.play.service

import android.util.Log
import com.ssafy.a602.game.api.RhythmApi
import com.ssafy.a602.game.play.dto.RhythmSaveRequest
import com.ssafy.a602.auth.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 하드 모드 리듬게임 데이터 업로드 서비스
 * 곡 종료 시 한 번에 업로드하는 API 호출
 */
@Singleton
class RhythmUploadService @Inject constructor(
    private val tokenManager: TokenManager,
    private val rhythmApi: RhythmApi
) {
    
    companion object {
        private const val TAG = "RhythmUploadService"
    }
    
    /**
     * 리듬게임 데이터 업로드
     * POST /api/v1/game/rhythm/play
     * 
     * @param request 리듬게임 데이터 요청
     * @return 업로드 성공 여부
     */
    suspend fun uploadRhythmData(
        request: RhythmSaveRequest
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "리듬 데이터 업로드 시작: musicId=${request.musicId}, segments=${request.allFrames.size}")
            Log.d(TAG, "요청 데이터 상세: segments=${request.allFrames.map { "${it.type}:${it.frames.size}개" }}")
            
            Log.d(TAG, "API 호출: POST /api/v1/game/rhythm/play")
            val response = rhythmApi.saveRhythm(request)
            
            if (response.isSuccessful) {
                Log.d(TAG, "리듬 데이터 업로드 성공: ${response.code()}")
                Result.success(true)
            } else {
                val errorBody = response.errorBody()?.string() ?: "No error body"
                val errorMessage = "업로드 실패: ${response.code()} ${response.message()}"
                Log.e(TAG, errorMessage)
                Log.e(TAG, "에러 응답 본문: $errorBody")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "리듬 데이터 업로드 중 오류 발생", e)
            Result.failure(e)
        }
    }
    
    /**
     * 리듬게임 데이터 업로드 (재시도 포함)
     * 
     * @param request 리듬게임 데이터 요청
     * @param maxRetries 최대 재시도 횟수 (기본값: 3)
     * @return 업로드 성공 여부
     */
    suspend fun uploadRhythmDataWithRetry(
        request: RhythmSaveRequest,
        maxRetries: Int = 3
    ): Result<Boolean> {
        var lastException: Throwable? = null
        
        repeat(maxRetries) { attempt ->
            val result = uploadRhythmData(request)
            if (result.isSuccess) {
                Log.d(TAG, "리듬 데이터 업로드 성공 (시도 ${attempt + 1}/${maxRetries})")
                return result
            } else {
                lastException = result.exceptionOrNull()
                Log.w(TAG, "리듬 데이터 업로드 실패 (시도 ${attempt + 1}/${maxRetries}): ${lastException?.message}")
                
                // 마지막 시도가 아니면 잠시 대기
                if (attempt < maxRetries - 1) {
                    kotlinx.coroutines.delay(1000L * (attempt + 1)) // 1초, 2초, 3초 대기
                }
            }
        }
        
        Log.e(TAG, "리듬 데이터 업로드 최종 실패 (${maxRetries}회 시도)")
        return Result.failure(lastException ?: Exception("업로드 실패"))
    }
}
