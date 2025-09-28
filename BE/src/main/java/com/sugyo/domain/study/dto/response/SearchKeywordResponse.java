package com.sugyo.domain.study.dto.response;

import com.sugyo.domain.study.entity.Vocabulary;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SearchKeywordResponse {

    @Schema(description = "단어 id", example = "1")
    private Long wordId;

    @Schema(description = "단어 의미", example = "안녕하세요")
    private String word;

    public static SearchKeywordResponse from(Vocabulary vocabulary){
        return SearchKeywordResponse.builder()
                .wordId(vocabulary.getId())
                .word(vocabulary.getWord())
                .build();
    }
}
