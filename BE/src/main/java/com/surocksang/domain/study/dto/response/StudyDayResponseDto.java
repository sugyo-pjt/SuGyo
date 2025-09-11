package com.surocksang.domain.study.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class StudyDayResponseDto {
    
    private Integer day;
    
    private List<StudyWordItemDto> items;
}