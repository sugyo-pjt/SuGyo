package com.surocksang.domain.game.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalTime;
import java.util.List;

@Getter
@Builder
public class GameChartResponseDto {
    private Integer segment;
    private LocalTime barStartedAt;
    private String lyrics;
    private List<CorrectAnswerDto> correct;

    @Getter
    @Builder
    public static class CorrectAnswerDto {
        private Integer correctStartedIndex;
        private Integer correctEndedIndex;
        private LocalTime actionStartedAt;
        private LocalTime actionEndedAt;
    }
}