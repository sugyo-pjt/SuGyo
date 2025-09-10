package com.surocksang.domain.game.controller;

import com.surocksang.domain.game.dto.response.MusicListResponseDto;
import com.surocksang.domain.game.dto.response.MusicUrlResponseDto;
import com.surocksang.domain.game.service.MusicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/game/rhythm")
@RequiredArgsConstructor
public class MusicController {
    private final MusicService musicService;

    @Operation(
            summary = "Music의 URL 값 조회",
            description = "Music ID에 해당하는 Music의 DOWNLOAD URL 값 조회"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Music Id에 해당하는 Music URL 가져오기 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                    name = "성공 예시",
                                    value =
                                    """
                                    {
                                       "musicUrl": "https://surocksang.s3.us-east-1.amazonaws.com/jinglebell"
                                    }
                                    """
                                    ),
                                    @ExampleObject(
                                    name = "실패 예시",
                                    value =
                                    """
                                   {
                                     "status": 404,
                                     "code": "GLOBAL-404-01",
                                     "message": "요청한 리소스를 찾을 수 없습니다."
                                   }
                                   """
                                   )
                            }
                    )
            )
    })
    @GetMapping("/music/{musicId}")
    public ResponseEntity<?> getMusicUrl(@PathVariable Long musicId) {
        MusicUrlResponseDto musicUrl = musicService.getMusic(musicId);
        return ResponseEntity.ok((musicUrl));
    }

    @Operation(
            summary = "곡 목록 조회",
            description = "등록된 모든 곡의 목록을 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "곡 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "성공 예시",
                                            value =
                                            """
                                            [
                                              {
                                                "id": 1,
                                                "title": "징글벨",
                                                "singer": "김진환",
                                                "songTime": "00:01:00",
                                                "albumImageUrl": "https://surocksang.s3.us-east-1.amazonaws.com/null"
                                                "myScore": 123123
                                              }
                                            ]
                                            """
                                    )
                            }
                    )
            )
    })
    @GetMapping("/music")
    public ResponseEntity<?> getAllMusic() {
        List<MusicListResponseDto> musicList = musicService.getAllMusic();
        return ResponseEntity.ok((musicList));
    }
}