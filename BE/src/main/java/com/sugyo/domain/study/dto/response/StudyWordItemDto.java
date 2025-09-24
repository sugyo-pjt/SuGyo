package com.sugyo.domain.study.dto.response;

import com.sugyo.domain.study.entity.DailyVocabulary;
import com.sugyo.domain.study.entity.Vocabulary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
@AllArgsConstructor
public class StudyWordItemDto {

    private Long wordId;

    private String word;

    private String description;

    private String videoUrl;

    private Set<String> sameMotionWord;

    public static StudyWordItemDto from(Vocabulary v, Set<String> sameMotionWord) {
        var m = v.getMotion();
        return new StudyWordItemDto(
                v.getId(),
                v.getWord(),
                m.getDescription(),
                m.getVideoUrl(),
                sameMotionWord
        );
    }
}
