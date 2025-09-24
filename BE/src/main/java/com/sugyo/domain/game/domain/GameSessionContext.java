package com.sugyo.domain.game.domain;

import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static com.sugyo.domain.game.domain.GameState.PLAYING;

@Getter
@ToString
public class GameSessionContext {

    private final String userId;
    private final Long musicId;
    private final String webSocketSessionId;

    private final AtomicInteger score;      // 누적 점수
    private final AtomicInteger combo;      // 현재 콤보
    private final AtomicInteger perfectCount; // Perfect 판정 횟수
    private final AtomicInteger goodCount;    // Good 판정 횟수
    private final AtomicInteger missCount;    // Miss 판정 횟수

    private volatile GameState gameState;   // 게임 상태 (volatile로 가시성 보장)
    private volatile Instant lastActivityTime; // 마지막 활동 시간 (타임아웃 처리용)


    public GameSessionContext(String userId, Long songId, String webSocketSessionId){
        this.userId = userId;
        this.musicId = songId;
        this.webSocketSessionId = webSocketSessionId;

        this.score = new AtomicInteger(0);
        this.combo = new AtomicInteger(0);
        this.perfectCount = new AtomicInteger(0);
        this.goodCount = new AtomicInteger(0);
        this.missCount = new AtomicInteger(0);

        this.gameState = PLAYING;
        this.lastActivityTime = Instant.now();
    }

    public boolean isPlaying(){
        return this.gameState == PLAYING;
    }

    public void applyJudgment(int points, Judgment judgment){
        this.score.addAndGet(points);
        recordActivity();

        switch (judgment){
            case PERFECT -> {
                this.combo.incrementAndGet();
                this.perfectCount.incrementAndGet();
            }
            case GOOD -> {
                this.combo.incrementAndGet();
                this.goodCount.incrementAndGet();
            }
            case MISS -> {
                this.combo.set(0);
                this.missCount.incrementAndGet();
            }
        }
    }

    public void changeState(GameState newState){
        this.gameState = newState;
        recordActivity();
    }

    private void recordActivity(){
        this.lastActivityTime = Instant.now();
    }
}
