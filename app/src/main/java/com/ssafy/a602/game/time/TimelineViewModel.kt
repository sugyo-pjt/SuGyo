package com.ssafy.a602.game.time

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * TimelineViewModel
 * 
 * ExoPlayer의 재생 상태를 관찰하고 게임에 필요한 정확한 타이밍 정보를 제공하는 ViewModel
 * 
 * 주요 기능:
 * - ExoPlayer의 현재 재생 위치와 상태를 실시간으로 관찰
 * - Choreographer를 통한 정밀한 타이밍 제공 (약 16ms 간격)
 * - UI에서 StateFlow를 통해 타이밍 정보 수집 가능
 */
class TimelineViewModel(
    player: Player // ExoPlayer 인스턴스
) : ViewModel() {

    // AudioClock: ExoPlayer의 재생 상태를 관찰하고 TimelineTick을 생성하는 핵심 클래스
    private val clock = AudioClock(player)

    // UI에서 바로 수집할 수 있도록 StateFlow로 노출
    // SharingStarted.WhileSubscribed(5_000): 구독자가 있을 때만 활성화, 5초 후 자동 정지
    val ticks: StateFlow<TimelineTick?> =
        clock.ticks.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null // 초기값: null
        )

    /**
     * TimelineViewModel 시작
     * Choreographer 콜백을 등록하여 정밀한 타이밍 시작
     */
    fun start() = clock.start()
    
    /**
     * TimelineViewModel 정지
     * Choreographer 콜백을 제거하여 리소스 정리
     */
    fun stop() = clock.stop()
}
