package com.ssafy.a602.game.preparation

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a602.game.data.GameDataManager
import com.ssafy.a602.game.songs.SongItem
import com.ssafy.a602.game.preparation.GamePreparationState
import com.ssafy.a602.game.preparation.ResourceLoadingState
import com.ssafy.a602.game.PermissionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GamePreparationViewModel : ViewModel() {
    
    private val _preparationState = MutableStateFlow<GamePreparationState>(GamePreparationState.LoadingResources)
    val preparationState = _preparationState.asStateFlow()
    
    private val _resourceLoadingState = MutableStateFlow(ResourceLoadingState())
    val resourceLoadingState = _resourceLoadingState.asStateFlow()
    
    val permissionState = PermissionManager.permissionState
    
    private val _countdownValue = MutableStateFlow(3)
    val countdownValue = _countdownValue.asStateFlow()
    
    // 권한 요청 런처 (Activity에서 설정)
    var permissionLauncher: ((Array<String>) -> Unit)? = null
    
    // 권한 거부 시 설정 앱 열기 함수
    var openSettings: (() -> Unit)? = null
    
    fun startPreparation(song: SongItem) {
        viewModelScope.launch {
            // GameDataManager에 곡 선택 저장
            GameDataManager.selectSong(song)
            
            // 1. 리소스 로딩 시작
            loadResources(song)
            
            // 2. 권한 상태 확인
            checkPermissions()
        }
    }
    
    private suspend fun loadResources(song: SongItem) {
        // 실제로는 여기서 오디오, 가사, 차트 파일을 로드
        // 시뮬레이션을 위해 지연 시간 추가
        
        delay(500) // 오디오 로드 시뮬레이션
        _resourceLoadingState.update { it.copy(audioLoaded = true) }
        
        delay(300) // 가사 로드 시뮬레이션
        _resourceLoadingState.update { it.copy(lyricsLoaded = true) }
        
        delay(400) // 차트 로드 시뮬레이션
        _resourceLoadingState.update { it.copy(chartLoaded = true) }
        
        // 리소스 로딩 완료 후 권한 상태 확인
        checkNextState()
    }
    
    private fun checkPermissions() {
        // 권한 상태는 Activity에서 업데이트됨
        checkNextState()
    }
    
    fun checkPermissions(context: Context) {
        PermissionManager.checkPermissions(context)
        checkNextState()
    }
    
    fun updatePermissionState(
        cameraGranted: Boolean,
        shouldShowRationale: Boolean = false
    ) {
        PermissionManager.updatePermissionState(
            cameraGranted = cameraGranted,
            shouldShowRationale = shouldShowRationale
        )
        checkNextState()
    }
    
    private fun checkNextState() {
        val resourceState = _resourceLoadingState.value
        val permissionState = permissionState.value
        
        when {
            !resourceState.isComplete -> {
                _preparationState.value = GamePreparationState.LoadingResources
            }
            !permissionState.isComplete -> {
                _preparationState.value = GamePreparationState.WaitingPermission
            }
            else -> {
                // 리소스와 권한 모두 준비 완료
                startCameraWarmup()
            }
        }
    }
    
    fun requestPermissions() {
        permissionLauncher?.invoke(arrayOf(
            android.Manifest.permission.CAMERA
        ))
    }
    
    fun startCameraWarmup() {
        viewModelScope.launch {
            _preparationState.value = GamePreparationState.WarmingUpCamera
            delay(1000) // 카메라 워밍업 시뮬레이션
            
            startCountdown()
        }
    }
    
    private fun startCountdown() {
        viewModelScope.launch {
            _preparationState.value = GamePreparationState.Countdown
            
            for (i in 3 downTo 1) {
                _countdownValue.value = i
                delay(1000)
            }
            
            // 카운트다운이 끝나면 바로 게임 시작
            _preparationState.value = GamePreparationState.Ready
            GameDataManager.startGame()
            onGameStart?.invoke()
        }
    }
    
    // 게임 시작 콜백
    var onGameStart: (() -> Unit)? = null
    
    fun onPermissionDenied(shouldShowRationale: Boolean) {
        if (shouldShowRationale) {
            _preparationState.value = GamePreparationState.Error("권한이 거부되었습니다. 다시 시도해주세요.")
        } else {
            _preparationState.value = GamePreparationState.Error("설정에서 권한을 허용해주세요.")
        }
    }
    
    fun retryPermission() {
        _preparationState.value = GamePreparationState.WaitingPermission
    }
    
    fun openAppSettings() {
        openSettings?.invoke()
    }
}
