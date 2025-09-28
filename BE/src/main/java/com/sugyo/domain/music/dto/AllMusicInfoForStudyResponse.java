package com.sugyo.domain.music.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class AllMusicInfoForStudyResponse {

    @Schema(description = "학습 가능한 모든 노래 정보")
    List<MusicInfoForStudy> musics;

}
