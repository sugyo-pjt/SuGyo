package com.sugyo.domain.game.dto.request;

import com.sugyo.domain.game.dto.EasyGameMotionFrame;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "게임 플레이 요청 DTO")
@Getter
@NoArgsConstructor
public class GamePlayRequestDto {

    @Schema(description = "음악 ID", example = "1")
    private Long musicId;

    @Schema(description = "세그먼트 번호", example = "1")
    private Integer segment;

    @Schema(description = "프레임 데이터 배열")
    private List<EasyGameMotionFrame> frames;
}