package com.ssafy.a602.web

import android.graphics.Color
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.gson.Gson
import com.ssafy.a602.chatbot.HandFrame3D

@Composable
fun ThreeDHandCanvas(
    frame: HandFrame3D?,
    modifier: Modifier = Modifier,
    assetUrl: String = "https://appassets.androidplatform.net/assets/www/index.html",
    throttleMs: Long = 33L
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var jsReady by remember { mutableStateOf(false) }
    val gson = remember { Gson() }
    var lastSent by remember { mutableStateOf(0L) }

    val webView = remember {
        WebView(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
            
            // WebView 렌더링 강제 활성화
            setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
            setWillNotDraw(false)
            
            // WebViewAssetLoader 설정
            val assetLoader = WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
                .build()
            
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            
            // 3D 렌더링을 위한 성능 최적화 설정
            settings.setRenderPriority(android.webkit.WebSettings.RenderPriority.HIGH)
            settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            settings.databaseEnabled = true
            
            // 하드웨어 가속 활성화
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
            }
            
            // WebView 레이아웃 설정 개선
            setClipToPadding(false)
            setClipChildren(false)
            setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY)
            
            // WebView 렌더링 강제 활성화
            setWillNotDraw(false)
            setWillNotCacheDrawing(false)
            setDrawingCacheEnabled(true)
            setDrawingCacheQuality(WebView.DRAWING_CACHE_QUALITY_HIGH)
            
            // 디버깅 활성화
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                WebView.setWebContentsDebuggingEnabled(true)
            }

            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView, 
                    request: WebResourceRequest
                ): WebResourceResponse? {
                    return assetLoader.shouldInterceptRequest(request.url)
                }
                
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    return false // 모든 URL을 WebView에서 처리
                }
                
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d("WebView3D", "페이지 로드 완료: $url")
                    
                    // WebView 크기 로깅 및 강제 레이아웃
                    view?.let { webView ->
                        webView.post {
                            Log.d("WebView3D", "WebView 크기: ${webView.width}x${webView.height}")
                            Log.d("WebView3D", "WebView 위치: ${webView.x}, ${webView.y}")
                            
                            // WebView 레이아웃 강제 갱신
                            webView.requestLayout()
                            webView.invalidate()
                            
                            // JavaScript에 WebView 크기 정보 전달
                            val jsCode = """
                                if (window.AndroidBridge && window.AndroidBridge.onWebViewReady) {
                                    window.AndroidBridge.onWebViewReady(${webView.width}, ${webView.height});
                                }
                            """.trimIndent()
                            webView.evaluateJavascript(jsCode, null)
                        }
                    }
                }
                
                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    Log.e("WebView3D", "WebView 오류: $errorCode - $description ($failingUrl)")
                }
            }

            addJavascriptInterface(object {
                @JavascriptInterface fun onReady() {
                    jsReady = true
                    Log.d("WebView3D", "JS bridge ready")
                }
                
                @JavascriptInterface fun onAvatarLoaded() {
                    Log.d("WebView3D", "아바타 모델 로드 완료")
                }
                
                @JavascriptInterface fun onRenderStart() {
                    Log.d("WebView3D", "3D 렌더링 시작")
                }
                
                @JavascriptInterface fun onWebViewReady(width: Int, height: Int) {
                    Log.d("WebView3D", "WebView 준비 완료 - 크기: ${width}x${height}")
                }
            }, "AndroidBridge")

            loadUrl(assetUrl)
        }
    }

    // 프레임 push (스로틀)
    LaunchedEffect(frame, jsReady) {
        if (!jsReady || frame == null) return@LaunchedEffect
        val now = SystemClock.uptimeMillis()
        if (now - lastSent >= throttleMs) {
            val json = gson.toJson(frame)
            webView.evaluateJavascript("window.updateHandFrame($json)", null)
            lastSent = now
        }
    }

    AndroidView(factory = { webView }, modifier = modifier)
}
