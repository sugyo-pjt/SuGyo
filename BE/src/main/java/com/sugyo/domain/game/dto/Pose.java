package com.sugyo.domain.game.dto;

import com.sugyo.domain.game.domain.BodyPart;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record Pose(
        @NotBlank(message = "신체 부위를 필수로 명시해야 합니다.")
        @Schema(description = "신체 부위", implementation = BodyPart.class)
        BodyPart part,

        @NotEmpty(message = "좌표 정보를 필수로 포함해야 합니다.")
        List<Coordinate> coordinates
) {
}
