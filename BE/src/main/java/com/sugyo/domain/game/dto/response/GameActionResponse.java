package com.sugyo.domain.game.dto.response;

import com.sugyo.domain.game.domain.GameSessionContext;
import com.sugyo.domain.game.domain.Judgment;

public record GameActionResponse(
        Judgment judgment,   // 판정 결과
        int points,        // 이번 판정으로 획득한 점수
        int combo,         // 현재 콤보
        int totalScore,    // 누적 총점
        int perfectCount,
        int goodCount,
        int missCount
) {
    public static GameActionResponse from(GameSessionContext context, Judgment judgment, int points){
        return new GameActionResponse(
                judgment,
                points,
                context.getCombo().get(),
                context.getScore().get(),
                context.getPerfectCount().get(),
                context.getGoodCount().get(),
                context.getMissCount().get()
        );
    }
}
