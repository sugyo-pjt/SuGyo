package com.sugyo.domain.study.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "퀴즈 결과 요청 DTO")
public class QuizResultRequest {

    @Schema(description = "학습 날짜 ID", example = "1")
    private Long dayId;

    @Schema(description = "맞춘 단어 개수", example = "6")
    private Integer score;
}