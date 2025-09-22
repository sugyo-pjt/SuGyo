package com.sugyo.domain.game.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class MusicRankingResponseDto {
    private Long musicId;
    private String musicTitle;
    private List<RankingUserDto> ranking;
    private MyRankInfoDto myInfo;
}