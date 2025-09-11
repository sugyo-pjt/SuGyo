package com.ssafy.a602

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.ssafy.a602.ui.theme.S13P21A602Theme
import com.ssafy.a602.MainScreen
import com.ssafy.a602.game.PermissionManager

class MainActivity : ComponentActivity() {
    
    private lateinit var snackbarHostState: SnackbarHostState
    
    // 권한 요청 런처
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        
        // ViewModel에 권한 상태 업데이트
        updatePermissionState(cameraGranted)
        
        // 권한이 거부된 경우 처리
        if (!cameraGranted) {
            handlePermissionDenied()
        }
    }
    
    // 설정 앱 열기 런처
    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // 설정에서 돌아온 후 권한 상태 재확인
        checkPermissions()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        snackbarHostState = SnackbarHostState()
        
        // 초기 권한 상태 확인
        PermissionManager.checkPermissions(this)
        
        setContent {
            S13P21A602Theme {
                MainScreen(
                    permissionLauncher = { permissions ->
                        permissionLauncher.launch(permissions)
                    },
                    snackbarHostState = snackbarHostState,
                    openSettings = { openAppSettings() }
                )
            }
        }
    }
    
    private fun updatePermissionState(cameraGranted: Boolean) {
        PermissionManager.updatePermissionState(
            cameraGranted = cameraGranted
        )
    }
    
    private fun handlePermissionDenied() {
        val shouldShowRationale = shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
        
        // 권한 거부 처리
        
        if (shouldShowRationale) {
            // 사용자가 "다시 묻지 않음"을 선택하지 않은 경우
            showPermissionDeniedSnackbar("권한이 거부되었습니다. 다시 시도해주세요.")
        } else {
            // 사용자가 "다시 묻지 않음"을 선택한 경우
            showPermissionDeniedSnackbar("설정에서 권한을 허용해주세요.")
        }
    }
    
    private fun showPermissionDeniedSnackbar(message: String) {
        // Snackbar 표시 로직
    }
    
    private fun checkPermissions() {
        val cameraGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        
        updatePermissionState(cameraGranted)
    }
    
    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        settingsLauncher.launch(intent)
    }
    
}