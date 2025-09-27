package com.sugyo.domain.game.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class AllFramesDto {
    private Long musicId;
    private List<GameActionRequest> allFrames;
}