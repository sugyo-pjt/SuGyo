package com.ssafy.a602.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.sp
import com.ssafy.a602.game.data.GameDataManager

/* ========== Preview ========== */

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
    
    // Preview용 더미 데이터 설정
    LaunchedEffect(Unit) {
        val sampleSong = Song(
            id = "way_back_home",
            title = "WAY BACK HOME",
            artist = "SHAUN",
            durationText = "3:14",
            bpm = 120,
            rating = 4.2,
            bestScore = 89650,
            thumbnailRes = null,
            audioUrl = "https://www.soundjay.com/misc/sounds/bell-ringing-05.wav" // Preview용 더미 URL
        )
        GameDataManager.selectSong(sampleSong)
        GameDataManager.startGame()
    }
    
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

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    backgroundColor = 0xFF0D1118
)
@Composable
fun GamePlayScreenPerfectPreview() {
    // Preview용 더미 데이터 설정
    LaunchedEffect(Unit) {
        val sampleSong = Song(
            id = "way_back_home",
            title = "WAY BACK HOME",
            artist = "SHAUN",
            durationText = "3:14",
            bpm = 120,
            rating = 4.2,
            bestScore = 89650,
            thumbnailRes = null,
            audioUrl = "https://www.soundjay.com/misc/sounds/bell-ringing-05.wav" // Preview용 더미 URL
        )
        GameDataManager.selectSong(sampleSong)
        GameDataManager.startGame()
    }
    
    GamePlayScreen(
        songId = "way_back_home",
        isPaused = false,
        onTogglePause = { },
        onGameComplete = {},
        onGameQuit = {},
        onOpenSettings = {},
        judgmentResult = JudgmentResult.Perfect
    )
}

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    backgroundColor = 0xFF0D1118
)
@Composable
fun GamePlayScreenMissPreview() {
    // Preview용 더미 데이터 설정
    LaunchedEffect(Unit) {
        val sampleSong = Song(
            id = "way_back_home",
            title = "WAY BACK HOME",
            artist = "SHAUN",
            durationText = "3:14",
            bpm = 120,
            rating = 4.2,
            bestScore = 89650,
            thumbnailRes = null,
            audioUrl = "https://www.soundjay.com/misc/sounds/bell-ringing-05.wav" // Preview용 더미 URL
        )
        GameDataManager.selectSong(sampleSong)
        GameDataManager.startGame()
    }
    
    GamePlayScreen(
        songId = "way_back_home",
        isPaused = false,
        onTogglePause = { },
        onGameComplete = {},
        onGameQuit = {},
        onOpenSettings = {},
        judgmentResult = JudgmentResult.Miss
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
