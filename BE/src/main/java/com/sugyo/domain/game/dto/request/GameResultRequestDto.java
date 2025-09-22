package com.sugyo.domain.game.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GameResultRequestDto {
    private Long musicId;
    private Integer score;
}