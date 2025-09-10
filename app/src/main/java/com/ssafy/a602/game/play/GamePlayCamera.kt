package com.ssafy.a602.game

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.util.concurrent.Executors

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    lensFacing: Int = CameraSelector.LENS_FACING_FRONT,
    enableAnalysis: Boolean = false,
    mirrorPreview: Boolean = true, // 미러 ON 기본
    onFrame: ((ImageProxy) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Preview 모드에서는 카메라를 사용하지 않고 플레이스홀더 표시
    val isPreviewMode = context.javaClass.name.contains("Preview") ||
                       context.javaClass.name.contains("ComposeViewAdapter")

    if (isPreviewMode) {
        // Preview 모드에서는 카메라 아이콘만 표시
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Outlined.CameraAlt,
                    contentDescription = "카메라",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "수어 인식 카메라",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        return
    }

    val controller = remember {
        try {
            LifecycleCameraController(context).apply {
                cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()
            }
        } catch (e: Exception) {
            // 카메라 초기화 실패 시 null 반환
            null
        }
    }

    // controller가 null이면 플레이스홀더 표시
    if (controller == null) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Outlined.CameraAlt,
                    contentDescription = "카메라",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "카메라를 사용할 수 없습니다",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        return
    }

    // 1) Executor를 한번만 만들고, 화면 사라질 때 종료
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose { analyzerExecutor.shutdown() }
    }

    // 2) 분석기 on/off를 수명에 맞춰 관리
    DisposableEffect(enableAnalysis, onFrame) {
        if (enableAnalysis && onFrame != null) {
            controller.setImageAnalysisBackpressureStrategy(
                ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
            )
            controller.setImageAnalysisAnalyzer(analyzerExecutor) { image ->
                try {
                    onFrame(image)
                } finally {
                    image.close()
                }
            }
            controller.setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
        } else {
            controller.clearImageAnalysisAnalyzer()
            controller.setEnabledUseCases(0) // 분석 끔 (필요 시 PREVIEW만 켜도 됨)
        }
        onDispose {
            // 해제 시점에 안전하게 클리어
            controller.clearImageAnalysisAnalyzer()
        }
    }

    // 3) 실제 프리뷰 붙이기
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
                // 전면 카메라일 때 거울 효과
                if (lensFacing == CameraSelector.LENS_FACING_FRONT && mirrorPreview) {
                    scaleX = -1f
                }
                controller.bindToLifecycle(lifecycleOwner)
                this.controller = controller
            }
        },
        update = { previewView ->
            // lensFacing 또는 mirrorPreview 바뀌면 반영
            previewView.scaleX =
                if (lensFacing == CameraSelector.LENS_FACING_FRONT && mirrorPreview) -1f else 1f
        }
    )
}
