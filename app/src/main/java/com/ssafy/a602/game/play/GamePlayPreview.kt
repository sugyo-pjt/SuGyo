package com.ssafy.a602.game.play

import androidx.camera.core.ExperimentalMirrorMode
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlin.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.a602.game.play.JudgmentResult
import com.ssafy.a602.game.songs.SongItem
import com.ssafy.a602.game.play.GamePlayScreen
import androidx.compose.ui.unit.sp
import com.ssafy.a602.game.data.GameDataManager
import com.ssafy.a602.game.play.JudgmentType

/* ========== Preview ========== */

@OptIn(ExperimentalMirrorMode::class)
@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    backgroundColor = 0xFF0D1118
)
@Composable
fun GamePlayScreenPreview() {
    var paused by remember { mutableStateOf(false) }
    var judgment by remember { mutableStateOf<JudgmentResult?>(null) }
    
    GamePlayScreen(
        songId = "way_back_home",
        isPaused = paused,
        onTogglePause = { paused = !paused },
        onGameComplete = {},
        onGameQuit = {},
        onOpenSettings = {},
        judgmentResult = judgment
    )
}

@OptIn(ExperimentalMirrorMode::class)
@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    backgroundColor = 0xFF0D1118
)
@Composable
fun GamePlayScreenPerfectPreview() {
    
    GamePlayScreen(
        songId = "way_back_home",
        isPaused = false,
        onTogglePause = { },
        onGameComplete = {},
        onGameQuit = {},
        onOpenSettings = {},
        judgmentResult = JudgmentResult(
            type = JudgmentType.PERFECT,
            accuracy = 0.95f,
            score = 1000,
            combo = 5,
            timestamp = System.currentTimeMillis()
        )
    )
}

@OptIn(ExperimentalMirrorMode::class)
@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    backgroundColor = 0xFF0D1118
)
@Composable
fun GamePlayScreenMissPreview() {
    
    GamePlayScreen(
        songId = "way_back_home",
        isPaused = false,
        onTogglePause = { },
        onGameComplete = {},
        onGameQuit = {},
        onOpenSettings = {},
        judgmentResult = JudgmentResult(
            type = JudgmentType.MISS,
            accuracy = 0.0f,
            score = 0,
            combo = 0,
            timestamp = System.currentTimeMillis()
        )
    )
}

// 간단한 판정 결과만 보여주는 Preview
@Preview(
    showBackground = true,
    widthDp = 200,
    heightDp = 200,
    backgroundColor = 0xFF0D1118
)
@Composable
fun JudgmentOverlayPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF151B24))
            .border(3.dp, Color(0xFF2BD46D), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "PERFECT",
            color = Color(0xFF3B82F6),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(
    showBackground = true,
    widthDp = 200,
    heightDp = 200,
    backgroundColor = 0xFF0D1118
)
@Composable
fun JudgmentOverlayMissPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF151B24))
            .border(3.dp, Color(0xFF2BD46D), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "MISS",
            color = Color(0xFFFF5A5A),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
