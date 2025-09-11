package com.surocksang.domain.study.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class DayProgressDto {
    private Long dayId;

    private Integer day;

    private Integer correctCount;

    private Integer totalCount;
}