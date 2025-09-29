package com.ssafy.a602.game

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.ssafy.a602.game.preparation.PermissionState

object PermissionManager {
    private val _permissionState = MutableStateFlow(PermissionState())
    val permissionState = _permissionState.asStateFlow()
    
    fun checkPermissions(context: Context) {
        val cameraGranted = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        
        _permissionState.value = PermissionState(
            cameraGranted = cameraGranted
        )
    }
    
    fun updatePermissionState(
        cameraGranted: Boolean,
        shouldShowRationale: Boolean = false
    ) {
        _permissionState.value = PermissionState(
            cameraGranted = cameraGranted,
            shouldShowRationale = shouldShowRationale
        )
    }
}
