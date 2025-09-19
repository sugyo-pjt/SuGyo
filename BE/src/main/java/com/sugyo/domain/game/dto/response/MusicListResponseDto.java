package com.sugyo.domain.game.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalTime;

@Getter
@Builder
public class MusicListResponseDto {
    private Long id;
    private String title;
    private String singer;
    private LocalTime songTime;
    private String albumImageUrl;
    private Long myScore;
}