package com.sugyo.domain.game.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class GamePlayRequestDto {
    private Long musicId;
    private Integer segment;
    private List<Object> frames;
}