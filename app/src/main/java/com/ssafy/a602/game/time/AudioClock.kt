package com.ssafy.a602.game.time

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Choreographer
import androidx.media3.common.Player
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * AudioClock
 * 
 * ExoPlayer의 재생 상태를 실시간으로 관찰하고 정밀한 타이밍 정보를 제공하는 핵심 클래스
 * 
 * 주요 기능:
 * - Choreographer를 통한 프레임 기반 타이밍 (약 16ms 간격)
 * - ExoPlayer의 현재 재생 위치와 상태를 실시간으로 추적
 * - Pause→Resume 시 첫 틱 오차 최소화 (15ms 이하 목표)
 * - TimelineTick을 SharedFlow로 방출하여 UI에서 수집 가능
 */
class AudioClock(
    private val player: Player // ExoPlayer 인스턴스
) : Player.Listener {

    // 메인 스레드 핸들러: UI 스레드에서 작업 실행
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // Choreographer: Android의 프레임 동기화 시스템
    // 화면의 vsync에 맞춰 콜백을 실행하여 정밀한 타이밍 제공
    private val choreographer = Choreographer.getInstance()

    // TimelineTick을 방출하는 SharedFlow
    // extraBufferCapacity = 8: 버퍼 오버플로우 방지
    private val _ticks = MutableSharedFlow<TimelineTick>(
        extraBufferCapacity = 8
    )
    val ticks: SharedFlow<TimelineTick> = _ticks

    // AudioClock 실행 상태
    private var running = false

    /**
     * Choreographer 프레임 콜백
     * 화면의 vsync에 맞춰 실행되어 정밀한 타이밍 제공
     */
    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            
            // 현재 프레임 타이밍에 맞춰 TimelineTick 방출
            emitNow()
            
            // 다음 프레임을 위해 콜백 재등록
            choreographer.postFrameCallback(this)
        }
    }

    /**
     * AudioClock 시작
     * ExoPlayer 리스너 등록 및 Choreographer 콜백 시작
     */
    fun start() {
        if (running) {
            Log.w("AudioClock", "이미 실행 중입니다")
            return
        }
        
        Log.d("AudioClock", "AudioClock 시작")
        running = true
        
        // ExoPlayer 상태 변화 감지를 위한 리스너 등록
        player.addListener(this)
        
        // Choreographer 콜백 시작: 프레임 기반 타이밍 시작
        choreographer.postFrameCallback(frameCallback)
        
        Log.d("AudioClock", "AudioClock 시작 완료")
    }

    /**
     * AudioClock 정지
     * ExoPlayer 리스너 제거 및 Choreographer 콜백 정지
     */
    fun stop() {
        if (!running) {
            Log.w("AudioClock", "이미 정지된 상태입니다")
            return
        }
        
        Log.d("AudioClock", "AudioClock 정지")
        running = false
        
        // ExoPlayer 리스너 제거
        player.removeListener(this)
        
        // Choreographer 콜백 제거
        choreographer.removeFrameCallback(frameCallback)
        
        Log.d("AudioClock", "AudioClock 정지 완료")
    }

    /**
     * 현재 시점의 TimelineTick 생성 및 방출
     * ExoPlayer의 현재 상태를 기반으로 정확한 타이밍 정보 제공
     */
    private fun emitNow() {
        val tick = TimelineTick(
            positionMs = player.currentPosition, // ExoPlayer의 현재 재생 위치 (밀리초)
            isPlaying = player.isPlaying,        // ExoPlayer의 재생 상태
            wallClockMs = SystemClock.elapsedRealtime() // 시스템 경과 시간 (디버깅용)
        )
        
        // SharedFlow에 TimelineTick 방출
        _ticks.tryEmit(tick)
    }

    /**
     * ExoPlayer 재생 상태 변화 감지
     * Pause→Resume 시 즉시 틱 방출하여 첫 틱 오차 최소화 (15ms 이하 목표)
     */
    override fun onIsPlayingChanged(isPlaying: Boolean) {
        Log.d("AudioClock", "ExoPlayer 재생 상태 변화: isPlaying=$isPlaying")
        
        if (isPlaying) {
            // 메인 큐 최전위로 즉시 방출 → 첫 틱 지연 최소화
            Log.d("AudioClock", "Pause→Resume 감지, 즉시 틱 방출")
            mainHandler.postAtFrontOfQueue { emitNow() }
        }
    }
}
