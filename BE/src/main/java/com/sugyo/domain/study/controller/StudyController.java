package com.sugyo.domain.study.controller;

import com.sugyo.auth.dto.CustomUserDetails;
import com.sugyo.domain.study.dto.response.StudyProgressResponseDto;
import com.sugyo.domain.study.dto.response.StudyProgressDetailsResponseDto;
import com.sugyo.domain.study.dto.response.StudyDayResponseDto;
import com.sugyo.domain.study.dto.response.StudyWordItemDto;
import com.sugyo.domain.study.service.StudyService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "수어 학습", description = "수어 학습 API")
@RestController
@RequestMapping("/api/v1/study")
@RequiredArgsConstructor
public class StudyController {

    private final StudyService studyService;

    @Operation(
            summary = "학습 진행 상황 조회",
            description = "JWT 토큰을 통해 사용자의 학습 진행 상황을 조회합니다. 완료한 학습일 중 가장 높은 일수를 반환합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "학습 진행 상황 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "성공 예시",
                                            value =
                                            """
                                            {
                                              "progressDay": 5
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
                    description = "사용자를 찾을 수 없음",
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
    @GetMapping("/progress")
    public ResponseEntity<StudyProgressResponseDto> getStudyProgress(@AuthenticationPrincipal CustomUserDetails user) {
        StudyProgressResponseDto response = studyService.getStudyProgress(user);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "학습 진행 상황 상세 조회",
            description = "JWT 토큰을 통해 사용자의 학습 진행 상황을 상세 조회합니다. 전체 학습일과 각 일별 점수를 반환합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "학습 진행 상황 상세 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "성공 예시",
                                            value =
                                            """
                                            {
                                              "totalDays": 3,
                                              "progressDay": 2,
                                              "days": [
                                                {
                                                  "dayId": 1,
                                                  "day": 1,
                                                  "correctCount": 6,
                                                  "totalCount": 6
                                                },
                                                {
                                                  "dayId": 2,
                                                  "day": 2,
                                                  "correctCount": 7,
                                                  "totalCount": 7
                                                },
                                                {
                                                  "dayId": 3,
                                                  "day": 3,
                                                  "correctCount": null,
                                                  "totalCount": 3
                                                }
                                              ]
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
                    description = "사용자를 찾을 수 없음",
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
    @GetMapping("/progress/detail")
    public ResponseEntity<StudyProgressDetailsResponseDto> getStudyProgressDetails(@AuthenticationPrincipal CustomUserDetails user) {
        StudyProgressDetailsResponseDto response = studyService.getStudyProgressDetails(user);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "특정 일차 학습 내용 조회",
            description = "특정 일차의 학습 내용(단어 목록)을 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "학습 내용 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "성공 예시",
                                            value =
                                            """
                                            {
                                              "day": 1,
                                              "items": [
                                                {
                                                  "wordId": 1444,
                                                  "word": "안녕하세요",
                                                  "description": "왼손은 오른손 위에 두고 ...",
                                                  "videoUrl": "https://commondatastorage.googleapis.com/gtv-vi4"
                                                },
                                                {
                                                  "wordId": 1633,
                                                  "word": "나",
                                                  "description": "손바닥으로 자기자신을 가르키며...",
                                                  "videoUrl": "https://commondatastorage.googleapis.com/gtv-v"
                                                }
                                              ]
                                            }
                                            """
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "해당 일차를 찾을 수 없음",
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
    @GetMapping("/days/{dayId}")
    public ResponseEntity<StudyDayResponseDto> getStudyDay(@PathVariable Long dayId) {
        StudyDayResponseDto response = studyService.getStudyDay(dayId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search/{keyword}")
    public ResponseEntity<List<StudyWordItemDto>> searchVocabulary(@PathVariable String keyword){
        return ResponseEntity.ok(studyService.searchVocabulary(keyword));
    }

}
