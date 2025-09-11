package com.surocksang.domain.study.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class StudyProgressDetailsResponseDto {
    
    private Integer totalDays;
    
    private Integer progressDay;
    
    private List<DayProgressDto> days;
}