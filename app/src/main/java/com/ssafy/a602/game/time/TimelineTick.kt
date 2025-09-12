package com.ssafy.a602.game.time

/**
 * TimelineTick
 * 
 * ExoPlayer의 현재 재생 상태를 담고 있는 데이터 클래스
 * 게임의 정확한 타이밍을 위해 Choreographer를 통해 약 16ms마다 업데이트됨
 * 
 * @param positionMs ExoPlayer의 현재 재생 위치 (밀리초 단위)
 *                  - 게임 시간 계산의 기준이 되는 단일 시계
 *                  - player.currentPosition 값을 그대로 사용
 * 
 * @param isPlaying ExoPlayer의 현재 재생 상태
 *                 - true: 재생 중
 *                 - false: 일시정지 상태
 * 
 * @param wallClockMs 시스템 경과 시간 (밀리초 단위)
 *                   - 디버깅 및 성능 측정용
 *                   - SystemClock.elapsedRealtime() 값을 사용
 *                   - Pause→Resume 시 첫 틱 오차 측정에 활용
 */
data class TimelineTick(
    val positionMs: Long,     // 단일 시계: player.currentPosition
    val isPlaying: Boolean,   // ExoPlayer 재생 상태
    val wallClockMs: Long     // 디버깅/측정용 (elapsedRealtime)
)
