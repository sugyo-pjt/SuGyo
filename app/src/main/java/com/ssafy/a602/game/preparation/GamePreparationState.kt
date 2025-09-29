package com.ssafy.a602.game.preparation

// 게임 준비 단계 상태 정의
sealed class GamePreparationState {
    object LoadingResources : GamePreparationState() // 리소스 로딩 중
    object WaitingPermission : GamePreparationState() // 권한 대기 중
    object WarmingUpCamera : GamePreparationState() // 카메라 워밍업 중
    object Countdown : GamePreparationState() // 3,2,1 카운트다운
    object Ready : GamePreparationState() // 게임 시작 준비 완료
    data class Error(val message: String) : GamePreparationState() // 에러 상태
}

// 리소스 로딩 상태
data class ResourceLoadingState(
    val audioLoaded: Boolean = false,
    val lyricsLoaded: Boolean = false,
    val chartLoaded: Boolean = false
) {
    val isComplete: Boolean
        get() = audioLoaded && lyricsLoaded && chartLoaded
}

// 권한 상태
data class PermissionState(
    val cameraGranted: Boolean = false,
    val shouldShowRationale: Boolean = false
) {
    val isComplete: Boolean
        get() = cameraGranted
}
