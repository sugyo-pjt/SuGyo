package com.sugyo.domain.game.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class GameResultRequestDto {
    private List<AllFramesDto> clientCoordinates;
    private Long clientCalculateScore;
}