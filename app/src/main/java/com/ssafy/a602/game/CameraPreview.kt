package com.ssafy.a602.game

import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    lensFacing: Int = CameraSelector.LENS_FACING_FRONT,
    enableAnalysis: Boolean = false,
    onFrame: ((ImageProxy) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 최신 onFrame 참조 보존
    val onFrameState by rememberUpdatedState(onFrame)

    // 카메라 스레드
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { cameraExecutor.shutdown() } }

    // 한 번만 바인딩하도록 가드
    var bound by remember { mutableStateOf(false) }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            PreviewView(ctx).apply {
                // ✅ Compose 오버레이가 위에 보이도록 TextureView 모드 강제
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER

                // 전면카메라 미러링은 뷰 레벨에서만
                scaleX = if (lensFacing == CameraSelector.LENS_FACING_FRONT) -1f else 1f
            }
        },
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                runCatching {
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val selector = CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build()

                    val analysis: ImageAnalysis? =
                        if (enableAnalysis && onFrame != null) {
                            ImageAnalysis.Builder()
                                .setTargetResolution(android.util.Size(640, 480)) // 손 인식을 위한 적절한 해상도
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build().apply {
                                    setAnalyzer(cameraExecutor) { proxy ->
                                        try { onFrame.invoke(proxy) } finally { proxy.close() }
                                    }
                                }
                        } else null

                    cameraProvider.unbindAll()
                    if (analysis != null) {
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner, selector, preview, analysis
                        )
                    } else {
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner, selector, preview
                        )
                    }
                }.onFailure { e ->
                    android.util.Log.e("CameraPreview", "바인딩 실패", e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )

}
