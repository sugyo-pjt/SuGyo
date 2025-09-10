package com.surocksang.domain.game.controller;

import com.surocksang.common.dto.CommonResponse;
import com.surocksang.domain.game.dto.response.MusicListResponseDto;
import com.surocksang.domain.game.dto.response.MusicUrlResponseDto;
import com.surocksang.domain.game.service.MusicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
                                      "result": "success",
                                      "data": [
                                        {
                                          "musicUrl": "https://music-url"
                                        }
                                      ]
                                    }
                                    """
                                    ),
                                    @ExampleObject(
                                    name = "실패 예시",
                                    value =
                                    """
                                    {
                                       "result": "error",
                                       "data": {
                                         "status": 404,
                                         "code": "GLOBAL-404-01",
                                         "message": "요청한 리소스를 찾을 수 없습니다."
                                       }
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
        return ResponseEntity.ok(CommonResponse.success(musicUrl));
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
                                            {
                                              "result": "success",
                                              "data": [
                                                {
                                                  "id": 1,
                                                  "title": "곡 제목",
                                                  "singer": "가수명",
                                                  "songTime": "03:45",
                                                  "albumImageUrl": "https://album-image-url"
                                                }
                                              ]
                                            }
                                            """
                                    )
                            }
                    )
            )
    })
    @GetMapping("/music")
    public ResponseEntity<CommonResponse<List<MusicListResponseDto>>> getAllMusic() {
        List<MusicListResponseDto> musicList = musicService.getAllMusic();
        return ResponseEntity.ok(CommonResponse.success(musicList));
    }


}