package com.sugyo.domain.game.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "게임 모션 프레임 DTO")
public record EasyGameMotionFrame(

        @Schema(description = "몸체 포즈 좌표 (23개 랜드마크)")
        @NotEmpty(message = "몸체 포즈 좌표는 필수입니다.")
        @Valid
        List<Coordinate> pose,

        @Schema(description = "왼손 좌표 (21개 랜드마크)")
        @NotEmpty(message = "왼손 좌표는 필수입니다.")
        @Valid
        List<Coordinate> left,

        @Schema(description = "오른손 좌표 (21개 랜드마크)")
        @NotEmpty(message = "오른손 좌표는 필수입니다.")
        @Valid
        List<Coordinate> right
) {
}