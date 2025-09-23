package com.sugyo.domain.study.dto.response;

import com.sugyo.domain.study.entity.DailyVocabulary;
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

    // Factory method
    public static StudyWordItemDto from(DailyVocabulary dv) {
        var v = dv.getVocabulary();
        var m = v.getMotion();
        return new StudyWordItemDto(
                v.getId(),
                v.getWord(),
                m.getDescription(),
                m.getVideoUrl()
        );
    }
}