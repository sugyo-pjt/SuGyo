package com.sugyo.domain.game.dto.request;

import com.sugyo.domain.game.domain.GameActionType;
import com.sugyo.domain.game.dto.MotionFrame;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

@Schema(description = "게임 진행 요청 DTO")
public record GameActionRequest(

        @Schema(description = "요청 종류", implementation = GameActionType.class)
        @NotBlank(message = "요청 타입은 필수입니다.")
        GameActionType type,

        @Schema(description = "진행 시간(ms)", example = "1663843200300.0")
        @Positive(message = "타임스탬프는 양수여야 합니다.")
        double timestamp,

        @NotEmpty(message = "프레임은 필수로 포함해야 합니다.")
        @Valid
        List<MotionFrame> frames
) {
}
