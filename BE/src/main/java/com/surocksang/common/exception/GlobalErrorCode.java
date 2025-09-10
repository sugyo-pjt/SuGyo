package com.surocksang.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Getter
@RequiredArgsConstructor
public enum GlobalErrorCode implements ErrorCode {

    // 400
    INVALID_ARGUMENT(HttpStatus.BAD_REQUEST, "GLOBAL-400-01", "요청 인자값이 올바르지 않습니다."),
    MISSING_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "GLOBAL-400-02", "필수 요청 파라미터가 누락되었습니다."),
    INVALID_PARAMETER_FORMAT(HttpStatus.BAD_REQUEST, "GLOBAL-400-03", "요청 파라미터의 형식이 올바르지 않습니다."),
    INVALID_HTTP_METHOD(HttpStatus.BAD_REQUEST, "GLOBAL-400-04", "지원하지 않는 HTTP 메서드입니다."),
    // 401
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "GLOBAL-401-01", "인증이 필요합니다."),

    // 403
    FORBIDDEN(HttpStatus.FORBIDDEN, "GLOBAL-403-01", "접근 권한이 없습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "GLOBAL-404-01", "요청한 리소스를 찾을 수 없습니다."),

    // 500
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GLOBAL-500-01", "서버 내부에 예기치 못한 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    public static ErrorCode from(Exception ex) {
        if (ex instanceof MethodArgumentNotValidException) {
            return INVALID_ARGUMENT;
        }
        if (ex instanceof MissingServletRequestParameterException) {
            return MISSING_REQUIRED_PARAMETER;
        }
        if (ex instanceof MethodArgumentTypeMismatchException) {
            return INVALID_PARAMETER_FORMAT;
        }
        if (ex instanceof HttpRequestMethodNotSupportedException) {
            return INVALID_HTTP_METHOD;
        }
        if (ex instanceof NoResourceFoundException) {
            return RESOURCE_NOT_FOUND;
        }
        if (ex instanceof AuthenticationException) {
            return UNAUTHORIZED; // 401
        }
        if (ex instanceof AccessDeniedException) {
            return FORBIDDEN;    // 403
        }
        return INTERNAL_SERVER_ERROR;
    }
}
