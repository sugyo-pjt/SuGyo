package com.sugyo.domain.game.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.socket.CloseStatus;

@Getter
@RequiredArgsConstructor
public enum WebSocketErrorCode {

    // 4000번대: 클라이언트 요청 오류
    INVALID_URI_FORMAT(4001, "WS-4001", "연결 URI가 유효하지 않습니다."),
    SESSION_NOT_FOUND(4002, "WS-4002", "유효하지 않은 세션입니다."),
    INVALID_REQUEST_FORMAT(4003, "WS-4003", "요청 형식이 유효하지 않습니다."),
    INVALID_JSON_FORMAT(4004, "WS-4004", "유효하지 않은 JSON 형식입니다."),
    INVALID_MUSIC_ID(4005, "WS-4005", "유효하지 않은 MUSIC ID입니다."),

    // 4100번대: 게임 로직 오류
    GAME_LOGIC_ERROR(4101, "WS-4101", "게임 처리 중 오류가 발생했습니다."),
    PAUSE_TIMEOUT(4102, "WS-4102", "일시정지 타임아웃으로 인해 세션이 종료되었습니다."),
    IDLE_TIMEOUT(4103, "WS-4103", "연결 유휴 타임아웃으로 인해 세션이 종료되었습니다."),

    // 4400번대: 리소스 오류
    USER_NOT_FOUND_IN_SESSION(4401, "WS-4401", "세션에서 사용자를 찾지 못했습니다."),

    // 5000번대: 서버 내부 오류
    INTERNAL_SERVER_ERROR(5000, "WS-5000", "서버 내부 오류가 발생했습니다.");

    private final int closeCode;
    private final String code;
    private final String message;

    public CloseStatus toCloseStatus() {
        String reason = String.format("[%s] %s", this.code, this.message);
        return new CloseStatus(this.closeCode, reason);
    }
}
