package com.ssafy.a602.game

import android.content.Context
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import kotlin.OptIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * CameraPreview
 * 
 * CameraX를 사용한 카메라 프리뷰 컴포넌트
 * MediaPipe 분석을 위한 ImageAnalysis 기능 포함
 */
@OptIn(androidx.camera.core.ExperimentalMirrorMode::class)
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    lensFacing: Int = CameraSelector.LENS_FACING_FRONT,
    enableAnalysis: Boolean = false,
    onFrame: ((ImageProxy) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // 카메라 실행을 위한 Executor
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    // 카메라 생명주기 정리
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
    
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                // 전면 카메라일 때만 UI 미러링 (추론에는 영향 없음)
                if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                    scaleX = -1f
                }
            }
        },
        modifier = modifier.fillMaxSize(),
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                
                // Preview 설정 (미러 모드 비활성화)
                val preview = Preview.Builder()
                    .setMirrorMode(MirrorMode.MIRROR_MODE_OFF)
                    .build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                
                // CameraSelector 설정
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()
                
                // ImageAnalysis 설정 (MediaPipe 분석용)
                val imageAnalysis = if (enableAnalysis && onFrame != null) {
                    ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                onFrame(imageProxy)
                            }
                        }
                } else null
                
                try {
                    // 기존 바인딩 해제
                    cameraProvider.unbindAll()
                    
                    // 새로운 바인딩
                    val useCaseGroup = if (imageAnalysis != null) {
                        UseCaseGroup.Builder()
                            .addUseCase(preview)
                            .addUseCase(imageAnalysis)
                            .build()
                    } else {
                        UseCaseGroup.Builder()
                            .addUseCase(preview)
                            .build()
                    }
                    
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        useCaseGroup
                    )
                } catch (exc: Exception) {
                    android.util.Log.e("CameraPreview", "카메라 바인딩 실패", exc)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}
