package com.sugyo.domain.study.dto.response;

import com.sugyo.domain.study.entity.Vocabulary;
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

    public static StudyWordItemDto from(Vocabulary v){
        return StudyWordItemDto.builder()
                .wordId(v.getId())
                .word(v.getWord())
                .description(v.getDescription())
                .videoUrl(v.getVideoUrl())
                .build();
    }
}
