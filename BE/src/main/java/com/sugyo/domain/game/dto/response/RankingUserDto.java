package com.sugyo.domain.game.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class RankingUserDto {
    private Integer rank;
    private Long userId;
    private String userNickName;
    private String userProfileUrl;
    private Integer score;
    private LocalDateTime recordDate;
}