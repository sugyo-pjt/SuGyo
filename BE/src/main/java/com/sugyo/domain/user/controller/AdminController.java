package com.sugyo.domain.user.controller;

import com.sugyo.domain.user.dto.SignUpAdminRequest;
import com.sugyo.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "관리자 관리", description = "관리자 관련 API")
@Slf4j
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@RestController
public class AdminController {
    private final UserService userService;

    @Operation(summary = "회원가입(관리자)", description = "사용자의 정보(JSON)와 프로필 이미지(파일)를 받아 관리자로 등록합니다.")
    @PostMapping(value = "/signup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> signUpAdmin(@RequestPart("userInfo") @Valid SignUpAdminRequest request,
                                            @RequestPart(value = "profileImage", required = false) @Parameter(description = "프로필 이미지 파일") MultipartFile profileImage) {

        userService.signUpAdmin(request, profileImage);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
