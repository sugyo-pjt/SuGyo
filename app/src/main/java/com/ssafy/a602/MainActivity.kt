package com.ssafy.a602

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ssafy.a602.ui.theme.S13P21A602Theme
import com.ssafy.a602.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            S13P21A602Theme {
                MainScreen()
            }
        }
    }
}