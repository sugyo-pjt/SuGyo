package com.ssafy.a602.WebView   // 👈 폴더랑 정확히 일치해야 함

import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.ssafy.a602.R

class ThreeDWebActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        WebView.setWebContentsDebuggingEnabled(true)

        val wv = findViewById<WebView>(R.id.wv_main)

        if (Build.VERSION.SDK_INT >= 19) {
            wv.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        } else {
            wv.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }

        wv.webChromeClient = WebChromeClient()
        wv.webViewClient = WebViewClient()

        val settings: WebSettings = wv.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        @Suppress("DEPRECATION")
        settings.allowFileAccessFromFileURLs = true
        settings.allowUniversalAccessFromFileURLs = true
        settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true

        wv.settings.javaScriptEnabled = true
        wv.settings.allowFileAccessFromFileURLs = true
        wv.settings.allowUniversalAccessFromFileURLs = true
        wv.loadUrl("file:///android_asset/www/index.html")
    }
}
