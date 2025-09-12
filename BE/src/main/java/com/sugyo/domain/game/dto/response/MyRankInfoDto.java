package com.sugyo.domain.game.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class MyRankInfoDto {
    private Integer rank;
    private Integer score;
    private LocalDateTime recordDate;
}