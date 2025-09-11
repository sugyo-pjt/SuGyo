package com.surocksang.domain.study.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class StudyWordItemDto {
    
    private Long wordId;
    
    private String word;
    
    private String description;
    
    private String videoUrl;
}