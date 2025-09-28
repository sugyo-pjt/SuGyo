package com.sugyo.domain.game.service;

import com.sugyo.domain.game.domain.GameSessionContext;
import com.sugyo.domain.game.exception.WebSocketErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static com.sugyo.domain.game.domain.GameState.PAUSED;
import static com.sugyo.domain.game.exception.WebSocketErrorCode.IDLE_TIMEOUT;
import static com.sugyo.domain.game.exception.WebSocketErrorCode.PAUSE_TIMEOUT;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionTimeoutScheduler {
    private final WebSocketSessionManager sessionManager;

    private static final Duration PAUSE_TIMEOUT_DURATION = Duration.ofMinutes(10); // 일시정지 타임아웃: 10분
    private static final Duration IDLE_TIMEOUT_DURATION = Duration.ofMinutes(1);   // 유휴 타임아웃: 1분

    // 30초마다 유휴 세션 확인 및 정리
//    @Scheduled(fixedRate = 30_000)
//    public void checkSessionTimeouts(){
//        Instant now  = Instant.now();
//        Map<String, GameSessionContext> sessions = sessionManager.getSessions();
//        log.trace("유휴 세션 검사 시작... 현재 활성 세션 수: {}", sessions.size());
//
//        sessions.forEach((sessionId, context) -> {
//          Duration idleDuration = Duration.between(context.getLastActivityTime(), now);
//
//          if(context.getGameState() == PAUSED && 0 < idleDuration.compareTo(PAUSE_TIMEOUT_DURATION)){
//              log.debug("일시정지 타임아웃으로 세션 종료: UserId={}, IdleTime={}s, sessionId={}", context.getUserId(), idleDuration.getSeconds(), sessionId);
//              closeSessionWithError(context.getWebSocketSession(), PAUSE_TIMEOUT);
//              return;
//          }
//
//          if (0 < idleDuration.compareTo(IDLE_TIMEOUT_DURATION)){
//              log.debug("연결 유휴 타임아웃으로 세션 종료: UserId={}, IdleTime={}s, sessionId={}", context.getUserId(), idleDuration.getSeconds(), sessionId);
//              closeSessionWithError(context.getWebSocketSession(), IDLE_TIMEOUT);
//          }
//        });
//    }
//
//    private void closeSessionWithError(WebSocketSession session, WebSocketErrorCode errorCode) {
//        try {
//            if (session.isOpen()) {
//                session.close(errorCode.toCloseStatus());
//            }
//        } catch (IOException e) {
//            log.error("타임아웃으로 세션 종료 중 오류 발생: {}", session.getId());
//        }
//    }
}
