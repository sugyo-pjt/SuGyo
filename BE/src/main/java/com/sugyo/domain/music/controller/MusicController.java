package com.sugyo.domain.music.controller;

import com.sugyo.domain.music.dto.AllMusicInfoForStudyResponse;
import com.sugyo.domain.music.service.MusicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "노래 관리", description = "노래 관련 API")
@Slf4j
@RequestMapping("/api/v1/music")
@RequiredArgsConstructor
@RestController
public class MusicController {

    private final MusicService musicService;

    @Operation(summary = "학습용 노래 정보 리스트 조회", description = "학습 가능한 모든 노래들의 정보를 조회합니다.")
    @GetMapping("/study")
    public ResponseEntity<AllMusicInfoForStudyResponse> getAllMusicInfoForStudy(){
        AllMusicInfoForStudyResponse response = musicService.getAllMusicInfoForStudy();
        return ResponseEntity.ok(response);
    }

}

