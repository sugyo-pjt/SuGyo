// CameraPreview.kt
package com.ssafy.a602.game

import android.util.Size
import android.view.SurfaceView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
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
    onFrame: ((ImageProxy) -> Unit)? = null,
    /**
     * 특정 기기에서 전면 자동 미러링이 동작하지 않을 때만 true로 켜서 수동 미러(스케일) 사용.
     * 기본값은 false (권장). 일반적으로 CameraX가 전면 프리뷰를 자동으로 미러링합니다.
     */
    manualMirrorFallback: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val onFrameState by rememberUpdatedState(onFrame)

    // 카메라 전용 스레드
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { cameraExecutor.shutdown() } }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            PreviewView(ctx).apply {
                // TextureView 경로 강제 (뷰 변환/내부 트랜스폼 안정)
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER

                // ✅ 기본: 수동 미러링(scaleX) 적용 안 함
                //    (CameraX가 전면 카메라는 내부적으로 미러링합니다)
                //    단, 수동 강제가 필요한 특수 기기만 fallback 허용
                if (manualMirrorFallback && lensFacing == CameraSelector.LENS_FACING_FRONT) {
                    scaleX = -1f
                } else {
                    scaleX = 1f
                }

                android.util.Log.d(
                    "CameraPreview",
                    "factory: scaleX=$scaleX (mode=$implementationMode)"
                )
                logChildSurfaceType(this)
            }
        },
        update = { previewView ->
            // 재컴포지션 시에도 수동 미러링은 off가 기본
            if (manualMirrorFallback && lensFacing == CameraSelector.LENS_FACING_FRONT) {
                previewView.scaleX = -1f
            } else {
                previewView.scaleX = 1f
            }
            android.util.Log.d("CameraPreview", "update: scaleX=${previewView.scaleX}")

            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                runCatching {
                    val cameraProvider = cameraProviderFuture.get()

                    fun bind(previewViewToUse: PreviewView) {
                        val preview = Preview.Builder()
                            .build()
                            .also {
                                // 내부 미러링은 CameraX가 전면에서 자동 적용 (버전별 플래그 없음)
                                it.setSurfaceProvider(previewViewToUse.surfaceProvider)
                            }

                        val selector = CameraSelector.Builder()
                            .requireLensFacing(lensFacing)
                            .build()

                        val analysis: ImageAnalysis? =
                            if (enableAnalysis && onFrameState != null) {
                                ImageAnalysis.Builder()
                                    .setTargetResolution(Size(640, 480))
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build().apply {
                                        setAnalyzer(cameraExecutor) { proxy ->
                                            try { onFrameState?.invoke(proxy) }
                                            finally { proxy.close() }
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
                    }

                    // 1차 바인딩
                    bind(previewView)

                    // STREAMING 시점에 실제 붙은 자식 타입 점검 → SurfaceView면 TextureView로 재바인딩
                    previewView.previewStreamState.observe(lifecycleOwner) { state ->
                        if (state == PreviewView.StreamState.STREAMING) {
                            previewView.post {
                                val child = previewView.getChildAt(0)
                                android.util.Log.d(
                                    "CameraPreview",
                                    "STREAMING: child=${child?.javaClass?.simpleName}, mode=${previewView.implementationMode}"
                                )
                                if (child is SurfaceView) {
                                    previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                    android.util.Log.w("CameraPreview", "재바인딩: TextureView로 강제 전환")
                                    val cameraProvider = cameraProviderFuture.get()
                                    cameraProvider.unbindAll()
                                    bind(previewView)
                                }

                                // 최종 수동 미러링(필요 시에만)
                                if (manualMirrorFallback && lensFacing == CameraSelector.LENS_FACING_FRONT) {
                                    previewView.scaleX = -1f
                                } else {
                                    previewView.scaleX = 1f
                                }
                                android.util.Log.d("CameraPreview", "STREAMING: scaleX=${previewView.scaleX}")
                            }
                        }
                    }
                }.onFailure { e ->
                    android.util.Log.e("CameraPreview", "바인딩 실패", e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

private fun logChildSurfaceType(pv: PreviewView) {
    pv.post {
        val child = pv.getChildAt(0)
        android.util.Log.d(
            "CameraPreview",
            "PreviewView child=${child?.javaClass?.simpleName} (mode=${pv.implementationMode})"
        )
    }
}
