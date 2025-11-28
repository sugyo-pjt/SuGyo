package com.ssafy.a602.game.play.judge

import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

data class GameSessionContext(
    val userId: String,
    val musicId: Long,
    val webSocketSessionId: String,
    val score: AtomicInteger = AtomicInteger(0),
    val combo: AtomicInteger = AtomicInteger(0),
    val perfectCount: AtomicInteger = AtomicInteger(0),
    val goodCount: AtomicInteger = AtomicInteger(0),
    val missCount: AtomicInteger = AtomicInteger(0),
    var gameState: GameState = GameState.PLAYING,
    var lastActivityTime: Instant = Instant.now(),
    val lastNoteTimestamp: Float
) {
    
    constructor(userId: String, songId: Long, sessionId: String, lastNoteTimestamp: Float) : this(
        userId = userId,
        musicId = songId,
        webSocketSessionId = sessionId,
        score = AtomicInteger(0),
        combo = AtomicInteger(0),
        perfectCount = AtomicInteger(0),
        goodCount = AtomicInteger(0),
        missCount = AtomicInteger(0),
        gameState = GameState.PLAYING,
        lastActivityTime = Instant.now(),
        lastNoteTimestamp = lastNoteTimestamp
    )

    fun isPlaying(): Boolean {
        return this.gameState == GameState.PLAYING
    }

    fun applyJudgment(points: Int, judgment: Judgment) {
        this.score.addAndGet(points)
        recordActivity()

        when (judgment) {
            Judgment.PERFECT -> {
                this.combo.incrementAndGet()
                this.perfectCount.incrementAndGet()
            }
            Judgment.GOOD -> {
                this.combo.incrementAndGet()
                this.goodCount.incrementAndGet()
            }
            Judgment.MISS -> {
                this.combo.set(0)
                this.missCount.incrementAndGet()
            }
        }
    }

    fun changeState(newState: GameState) {
        this.gameState = newState
        recordActivity()
    }

    private fun recordActivity() {
        this.lastActivityTime = Instant.now()
    }
}

enum class GameState {
    PLAYING, PAUSED, FINISHED
}
