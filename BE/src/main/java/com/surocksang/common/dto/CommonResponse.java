package com.surocksang.common.dto;

import com.surocksang.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommonResponse<T> {
    private String result;
    private T data;

    // 성공 응답 생성 메서드
    public static <T> CommonResponse<T> success(T data) {
        return CommonResponse.<T>builder()
                .result("success")
                .data(data)
                .build();
    }

    // 실패 응답 생성 메서드 (문자열 메시지용)
    public static CommonResponse<String> error(String message) {
        return CommonResponse.<String>builder()
                .result("error")
                .data(message)
                .build();
    }

    // 실패 응답 생성 메서드 (ErrorResponse용)
    public static CommonResponse<ErrorResponse> error(ErrorCode errorCode) {
        return CommonResponse.<ErrorResponse>builder()
                .result("error")
                .data(new ErrorResponse(errorCode))
                .build();
    }

    // 실패 응답 생성 메서드 (ErrorResponse 직접 사용)
    public static CommonResponse<ErrorResponse> error(ErrorResponse errorResponse) {
        return CommonResponse.<ErrorResponse>builder()
                .result("error")
                .data(errorResponse)
                .build();
    }

    // 정보 응답 생성 메서드
    public static <T> CommonResponse<T> info(T data) {
        return CommonResponse.<T>builder()
                .result("info")
                .data(data)
                .build();
    }

    // 경고 응답 생성 메서드
    public static <T> CommonResponse<T> warning(T data) {
        return CommonResponse.<T>builder()
                .result("warning")
                .data(data)
                .build();
    }
}