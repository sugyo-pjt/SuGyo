package com.sugyo.domain.game.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalTime;

@Getter
@AllArgsConstructor
public class MusicWithScoreDto {
    private Long id;
    private String title;
    private String singer;
    private LocalTime songTime;
    private String albumImageUrl;
    private Integer myScore;
}