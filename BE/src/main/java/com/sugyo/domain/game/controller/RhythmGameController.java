package com.sugyo.domain.game.controller;

import com.sugyo.auth.dto.CustomUserDetails;
import com.sugyo.domain.game.dto.request.FrameSaveRequestDto;
import com.sugyo.domain.game.dto.request.GameActionRequest;
import com.sugyo.domain.game.dto.request.GamePlayRequestDto;
import com.sugyo.domain.game.dto.response.GameSimilarityResponseDto;
import com.sugyo.domain.game.dto.response.MusicChartResponseDto;
import com.sugyo.domain.game.dto.response.MusicListResponseDto;
import com.sugyo.domain.game.dto.response.MusicRankingResponseDto;
import com.sugyo.domain.game.dto.response.MusicUrlResponseDto;
import com.sugyo.domain.game.service.FrameCoordinatesService;
import com.sugyo.domain.game.service.RhythmGameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "리듬 게임", description = "리듬 게임 관련 API")
@RestController
@RequestMapping("/api/v1/game/rhythm")
@RequiredArgsConstructor
public class RhythmGameController {
    private final RhythmGameService rhythmGameService;
    private final FrameCoordinatesService frameCoordinatesService;

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
        MusicUrlResponseDto musicUrl = rhythmGameService.getMusic(musicId);
        return ResponseEntity.ok((musicUrl));
    }

    @Operation(
            summary = "곡 목록 조회",
            description = "JWT 토큰을 통해 사용자 인증 후 등록된 모든 곡의 목록과 사용자의 최고 점수를 조회합니다."
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
                                                                "albumImageUrl": "https://surocksang.s3.us-east-1.amazonaws.com/null",
                                                                "myScore": 123123
                                                              }
                                                            ]
                                                            """
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "실패 예시",
                                            value =
                                                    """
                                                            {
                                                              "status": 401,
                                                              "code": "AUTH-401-01",
                                                              "message": "인증에 실패했습니다."
                                                            }
                                                            """
                                    )
                            }
                    )
            )
    })
    @GetMapping("/music/list")
    public ResponseEntity<?> getAllMusic(@AuthenticationPrincipal CustomUserDetails user) {
        List<MusicListResponseDto> musicList = rhythmGameService.getAllMusicWithScore(user.getId());
        return ResponseEntity.ok((musicList));
    }

    @Operation(
            summary = "곡의 채보 정보 조회",
            description = "Music ID에 해당하는 곡의 채보 정보를 게임용 형식으로 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "곡 채보 정보 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "성공 예시",
                                            value =
                                                    """
                                                            [
                                                              {
                                                                "segment": 1,
                                                                "barStartedAt": "00:00:00",
                                                                "barEndedAt": "00:00:10",
                                                                "lyrics": "떳다 떳다 비행기",
                                                                "correct": [
                                                                  {
                                                                    "correctStartedIndex": 0,
                                                                    "correctEndedIndex": 1,
                                                                    "actionStartedAt": "00:00:03.123",
                                                                    "actionEndedAt": "00:00:04.123"
                                                                  }
                                                                ]
                                                              }
                                                            ]
                                                            """
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "곡을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
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
    @GetMapping("/music/{musicId}/chart")
    public ResponseEntity<?> getMusicChart(@PathVariable Long musicId) {
        List<MusicChartResponseDto> chartData = rhythmGameService.getMusicChart(musicId);
        return ResponseEntity.ok(chartData);
    }

    @Operation(
            summary = "곡별 랭킹 조회",
            description = "JWT 토큰을 통해 사용자 인증 후 특정 곡의 상위 5명 랭킹과 내 랭킹 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "곡별 랭킹 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "성공 예시",
                                            value =
                                                    """
                                                            {
                                                              "musicId": 123,
                                                              "musicTitle": "Hello Rhythm",
                                                              "ranking": [
                                                                {
                                                                  "rank": 1,
                                                                  "userId": 42,
                                                                  "userNickName": "Alice",
                                                                  "userProfileUrl": "https://example.com/alice.jpg",
                                                                  "score": 98000,
                                                                  "recordDate": "2025-09-12T10:30:00"
                                                                }
                                                              ],
                                                              "myInfo": {
                                                                "rank": 2,
                                                                "score": 96000,
                                                                "recordDate": "2025-09-12T11:00:00"
                                                              }
                                                            }
                                                            """
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "실패 예시",
                                            value =
                                                    """
                                                            {
                                                              "status": 401,
                                                              "code": "AUTH-401-01",
                                                              "message": "인증에 실패했습니다."
                                                            }
                                                            """
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "곡을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
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
    @GetMapping("/rank/{musicId}")
    public ResponseEntity<MusicRankingResponseDto> getMusicRanking(
            @PathVariable Long musicId,
            @AuthenticationPrincipal CustomUserDetails user) {
        MusicRankingResponseDto rankingData = rhythmGameService.getMusicRanking(musicId, user.getId());
        return ResponseEntity.ok(rankingData);
    }

//    @Operation(
//            summary = "게임 결과 저장",
//            description = "JWT 토큰을 통해 사용자 인증 후 게임 결과를 저장합니다. 기존 기록이 없으면 새로 생성하고, 있으면 최고 점수인지 확인 후 업데이트합니다."
//    )
//    @ApiResponses(value = {
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(
//                    responseCode = "200",
//                    description = "게임 결과 저장 성공",
//                    content = @Content(
//                            mediaType = "application/json",
//                            examples = {
//                                    @ExampleObject(
//                                            name = "성공 예시 - 최고 기록",
//                                            value =
//                                            """
//                                            {
//                                              "musicId": 1,
//                                              "isBestRecord": true
//                                            }
//                                            """
//                                    ),
//                                    @ExampleObject(
//                                            name = "성공 예시 - 기존 기록보다 낮음",
//                                            value =
//                                            """
//                                            {
//                                              "musicId": 1,
//                                              "isBestRecord": false
//                                            }
//                                            """
//                                    )
//                            }
//                    )
//            ),
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(
//                    responseCode = "401",
//                    description = "인증 실패",
//                    content = @Content(
//                            mediaType = "application/json",
//                            examples = {
//                                    @ExampleObject(
//                                            name = "실패 예시",
//                                            value =
//                                            """
//                                            {
//                                              "status": 401,
//                                              "code": "AUTH-401-01",
//                                              "message": "인증에 실패했습니다."
//                                            }
//                                            """
//                                    )
//                            }
//                    )
//            ),
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(
//                    responseCode = "404",
//                    description = "곡을 찾을 수 없음",
//                    content = @Content(
//                            mediaType = "application/json",
//                            examples = {
//                                    @ExampleObject(
//                                            name = "실패 예시",
//                                            value =
//                                            """
//                                            {
//                                              "status": 404,
//                                              "code": "GLOBAL-404-01",
//                                              "message": "요청한 리소스를 찾을 수 없습니다."
//                                            }
//                                            """
//                                    )
//                            }
//                    )
//            )
//    })
//    @PostMapping("/complete")
//    public ResponseEntity<GameResultResponseDto> saveGameResult(
//            @RequestBody GameResultRequestDto request,
//            @AuthenticationPrincipal CustomUserDetails user) {
//        GameResultResponseDto result = rhythmGameService.saveGameResult(request, user.getId());
//        return ResponseEntity.ok(result);
//    }

    //    @Operation(
//            summary = "게임 플레이 데이터 처리",
//            description = "JWT 토큰을 통해 사용자 인증 후 게임 플레이 중 발생하는 데이터를 처리합니다."
//    )
//    @ApiResponses(value = {
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(
//                    responseCode = "200",
//                    description = "게임 플레이 데이터 처리 성공",
//                    content = @Content(
//                            mediaType = "application/json",
//                            examples = {
//                                    @ExampleObject(
//                                            name = "성공 예시",
//                                            value = "\"OK\""
//                                    )
//                            }
//                    )
//            ),
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(
//                    responseCode = "401",
//                    description = "인증 실패",
//                    content = @Content(
//                            mediaType = "application/json",
//                            examples = {
//                                    @ExampleObject(
//                                            name = "실패 예시",
//                                            value =
//                                            """
//                                            {
//                                              "status": 401,
//                                              "code": "AUTH-401-01",
//                                              "message": "인증에 실패했습니다."
//                                            }
//                                            """
//                                    )
//                            }
//                    )
//            ),
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(
//                    responseCode = "404",
//                    description = "곡을 찾을 수 없음",
//                    content = @Content(
//                            mediaType = "application/json",
//                            examples = {
//                                    @ExampleObject(
//                                            name = "실패 예시",
//                                            value =
//                                            """
//                                            {
//                                              "status": 404,
//                                              "code": "GLOBAL-404-01",
//                                              "message": "요청한 리소스를 찾을 수 없습니다."
//                                            }
//                                            """
//                                    )
//                            }
//                    )
//            )
//    })
    @PostMapping("/play")
    public ResponseEntity<String> processGamePlay(
            @RequestBody GamePlayRequestDto request,
            @AuthenticationPrincipal CustomUserDetails user) {
        rhythmGameService.processGamePlay(request, user.getId());
        return ResponseEntity.ok("OK");
    }

    @Operation(
            summary = "프레임 좌표 데이터 저장",
            description = "프레임 좌표 데이터를 저장합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "프레임 좌표 데이터 저장 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "성공 예시",
                                            value = "\"Frame data saved successfully\""
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "음악을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
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
    @PostMapping("/save")
    public ResponseEntity<String> saveFrameCoordinates(
            @RequestBody FrameSaveRequestDto request) {
        frameCoordinatesService.saveFrameCoordinates(request);
        return ResponseEntity.ok("Frame data saved successfully");
    }

    @Operation(
            summary = "게임 동작 유사도 계산",
            description = "클라이언트에서 받은 게임 동작 데이터와 DB의 정답 프레임을 비교하여 유사도를 계산합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "유사도 계산 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "성공 예시",
                                            value =
                                                    """
                                                            {
                                                              "similarity": 0.8547,
                                                              "timestamp": 1663843200300.0,
                                                              "musicId": 1
                                                            }
                                                            """
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "해당 타임스탬프의 프레임 데이터를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "실패 예시",
                                            value =
                                                    """
                                                            {
                                                              "similarity": 0.0,
                                                              "timestamp": 300.0,
                                                              "musicId": 1
                                                            }
                                                            """
                                    )
                            }
                    )
            )
    })
    @PostMapping("/similarity")
    public ResponseEntity<GameSimilarityResponseDto> calculateSimilarity(
            @RequestBody GameActionRequest request) {
        double similarity = frameCoordinatesService.calculateSimilarity(request);
        GameSimilarityResponseDto response = new GameSimilarityResponseDto(
                similarity,
                request.timestamp(),
                1L
        );
        return ResponseEntity.ok(response);
    }
}
