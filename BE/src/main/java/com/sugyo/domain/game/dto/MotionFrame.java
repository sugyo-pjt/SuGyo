package com.sugyo.domain.game.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record MotionFrame(

        @NotNull(message = "프레임 인덱스 번호는 필수로 포함해야 합니다.")
        @Positive(message = "프레임 인덱스 번호는 양수여야 합니다.")
        @Schema(description = "요청 프레임들의 인덱스 번호", example = "1")
        Integer frame,

        @NotEmpty(message = "프레임 정보(좌표)를 필수로 포함해야 합니다.")
        @Valid
        List<Pose> poses
) {
}
