package com.sugyo.domain.game.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게임 유사도 계산 응답 DTO")
public record GameSimilarityResponseDto(
        @Schema(description = "유사도 점수 (0.0 ~ 1.0)", example = "0.8547")
        Double similarity,

        @Schema(description = "타임스탬프", example = "1663843200300.0")
        Double timestamp,

        @Schema(description = "음악 ID", example = "1")
        Long musicId
) {
}