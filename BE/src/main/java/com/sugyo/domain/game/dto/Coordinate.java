package com.sugyo.domain.game.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record Coordinate(

        @Schema(description = "x 좌표값", example = "0")
        double x,

        @Schema(description = "y 좌표값", example = "0")
        double y,

        @Schema(description = "z 좌표값", example = "0")
        double z,

        @Schema(description = "w 좌표값", example = "0")
        double w
) {
}
