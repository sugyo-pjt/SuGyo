package com.sugyo.domain.user.exception;

import com.sugyo.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    // 400
    REQUIRED_TERM_NOT_AGREED(HttpStatus.BAD_REQUEST, "USER-400-02", "필수 이용약관에 동의하지 않았습니다."),
    DUPLICATE_TERM_IN_REQUEST(HttpStatus.BAD_REQUEST, "TERM-400-03", "요청에 중복된 이용약관 항목이 존재합니다."),
    TERM_SET_MISMATCH(HttpStatus.BAD_REQUEST, "TERM-400-05", "요청한 이용약관 목록이 서버의 전체 약관 목록과 일치하지 않습니다."),

    // 404

    // 409
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "USER-409-01", "이미 사용 중인 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "USER-409-02", "이미 사용 중인 닉네임입니다."),
    INVALID_ADMIN_KEY(HttpStatus.CONFLICT, "USER-409-03", "존재하지 않는 admin key입니다.");


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
