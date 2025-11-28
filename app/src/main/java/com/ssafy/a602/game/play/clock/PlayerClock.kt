package com.ssafy.a602.game.play.clock

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 플레이어 시간 관리 클래스
 * ExoPlayer의 현재 위치를 캐시하여 안전하게 접근할 수 있도록 함
 */
class PlayerClock {
    private val TAG = "PlayerClock"
    
    // 플레이어 위치 제공자
    private var playerPositionProvider: (() -> Long)? = null
    
    // 현재 시간 캐시
    private val _currentTimeMs = MutableStateFlow(0L)
    val currentTimeMs = _currentTimeMs.asStateFlow()
    
    // 업데이트 작업
    private var updateJob: Job? = null
    
    /**
     * 플레이어 위치 제공자 설정
     * @param provider ExoPlayer의 currentPosition을 반환하는 함수
     */
    fun setPlayerPositionProvider(provider: () -> Long) {
        playerPositionProvider = provider
        Log.d(TAG, "플레이어 위치 제공자 설정됨")
    }
    
    /**
     * 시간 업데이트 시작
     * 메인 스레드에서 60Hz로 업데이트
     */
    fun start() {
        if (updateJob?.isActive == true) {
            Log.w(TAG, "이미 업데이트가 실행 중")
            return
        }
        
        updateJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                while (isActive) {
                    val position = playerPositionProvider?.invoke() ?: 0L
                    _currentTimeMs.value = position
                    
                    // 60Hz 업데이트 (약 16ms 간격)
                    delay(16)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.d(TAG, "시간 업데이트 정상 취소됨")
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "시간 업데이트 오류", e)
            }
        }
        
        Log.d(TAG, "시간 업데이트 시작됨")
    }
    
    /**
     * 시간 업데이트 중지
     */
    fun stop() {
        updateJob?.cancel()
        updateJob = null
        Log.d(TAG, "시간 업데이트 중지됨")
    }
    
    /**
     * 현재 시간 반환 (ms)
     * @return 현재 시간 (ms)
     */
    fun nowMs(): Long {
        return _currentTimeMs.value
    }
    
    /**
     * 현재 시간 반환 (초)
     * @return 현재 시간 (초)
     */
    fun nowSeconds(): Float {
        return nowMs() / 1000f
    }
    
    /**
     * 업데이트 상태 확인
     * @return 업데이트 중인지 여부
     */
    fun isRunning(): Boolean {
        return updateJob?.isActive == true
    }
}
