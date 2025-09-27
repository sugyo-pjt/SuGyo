package com.sugyo.domain.study.dto.response;

import com.sugyo.domain.study.entity.DailyVocabulary;
import com.sugyo.domain.study.entity.Vocabulary;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
@AllArgsConstructor
public class StudyWordItemDto {

    @Schema(description = "단어 id", example = "4118")
    private Long wordId;

    @Schema(description = "단어 뜻", example = "희다")
    private String word;

    @Schema(description = "동작 설명", example = "오른 주먹의 1지를 펴서 끝으로 치아를 가리킨다.")
    private String description;

    @Schema(description = "영상 url", example = "http://sldict.korean.go.kr/multimedia/multimedia_files/convert/20200824/735147/MOV000244266_700X466.mp4")
    private String videoUrl;

    @Schema(description = "유사 단어 목록", example = "[\n" +
            "      \"백\",\n" +
            "      \"하양\",\n" +
            "      \"흰색\",\n" +
            "      \"이\",\n" +
            "      \"치아\",\n" +
            "      \"하얗다\",\n" +
            "      \"이빨\",\n" +
            "      \"허옇다\",\n" +
            "      \"백색\"\n" +
            "    ]")
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
