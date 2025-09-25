package com.sugyo.domain.game.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sugyo.domain.game.domain.GameActionType;
import com.sugyo.domain.game.domain.GameSessionContext;
import com.sugyo.domain.game.domain.PlayValidationGroup;
import com.sugyo.domain.game.dto.request.GameActionRequest;
import com.sugyo.domain.game.exception.WebSocketErrorCode;
import com.sugyo.domain.game.exception.WebSocketException;
import com.sugyo.domain.game.service.WebSocketGameService;
import com.sugyo.domain.game.service.WebSocketSessionManager;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

import static com.sugyo.domain.game.exception.WebSocketErrorCode.INTERNAL_SERVER_ERROR;
import static com.sugyo.domain.game.exception.WebSocketErrorCode.INVALID_JSON_FORMAT;
import static com.sugyo.domain.game.exception.WebSocketErrorCode.INVALID_REQUEST_FORMAT;
import static com.sugyo.domain.game.exception.WebSocketErrorCode.INVALID_URI_FORMAT;
import static com.sugyo.domain.game.exception.WebSocketErrorCode.SESSION_NOT_FOUND;
import static com.sugyo.domain.game.exception.WebSocketErrorCode.USER_NOT_FOUND_IN_SESSION;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final WebSocketGameService gameService;

    private final WebSocketSessionManager sessionManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        try {
            Long musicId = extractMusicIdFromUri(session.getUri());
            String userId = extractUserIdFromSession(session);

            GameSessionContext context = gameService.initializeGameSession(userId, musicId, session);

            sessionManager.addSession(session, context);
            log.info("세션 시작: SessionId={}, UserId={}, MusicId={}", session.getId(), userId, musicId);
        } catch (WebSocketException e) {
            log.warn("WebSocket 예외 발생: Code={}, Message={}", e.getErrorCode().getCode(), e.getMessage());
            closeSessionWithError(session, e.getErrorCode());
        } catch (Exception e) {
            log.error("예측하지 못한 서버 오류 발생: SessionId={}", session.getId(), e);
            closeSessionWithError(session, INTERNAL_SERVER_ERROR);
        }
    }

    private Long extractMusicIdFromUri(URI uri) throws WebSocketException {
        String path = uri.getPath();
        try {
            return Long.parseLong(path.substring(path.lastIndexOf('/') + 1));
        } catch (NumberFormatException e) {
            throw new WebSocketException(INVALID_URI_FORMAT);
        }
    }

    private String extractUserIdFromSession(WebSocketSession session) throws WebSocketException {
        Object userId = session.getAttributes().get("userId");
        if (userId == null) {
            throw new WebSocketException(USER_NOT_FOUND_IN_SESSION);
        }
        return String.valueOf(userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            log.debug("웹소켓 메시지 도착: {}", message);
            GameSessionContext context = sessionManager.getContext(session);
            if (context == null) {
                throw new WebSocketException(SESSION_NOT_FOUND);
            }
            GameActionRequest request = parseAndValidate(message);
            log.debug("웹소켓 request type: {}", request.type());

            boolean isFinished = false;
            switch (request.type()) {
                case PLAY -> {
                    isFinished = gameService.processPlay(context, request);
                }
                case PAUSE -> gameService.pauseGame(context);
                case RESUME -> gameService.resumeGame(context);
            }
            if (isFinished) {
                session.close();
                sessionManager.removeSession(session);
                log.debug("웹소켓 연결 종료 완료: {}", session.getId());
            }
        } catch (WebSocketException e) {
            log.warn("WebSocket 예외 발생: Code={}, Message={}", e.getErrorCode().getCode(), e.getMessage());
            closeSessionWithError(session, e.getErrorCode());
        } catch (Exception e) {
            log.error("예측하지 못한 서버 오류 발생: SessionId={}", session.getId(), e);
            closeSessionWithError(session, INTERNAL_SERVER_ERROR);
        }
    }

    private GameActionRequest parseAndValidate(TextMessage message) throws WebSocketException {
        try {
            GameActionRequest request = objectMapper.readValue(message.getPayload(), GameActionRequest.class);
            Set<ConstraintViolation<GameActionRequest>> violations;

            if (request.type() == GameActionType.PLAY) {
                violations = validator.validate(request, PlayValidationGroup.class);
            } else {
                violations = validator.validate(request);
            }

            if (!violations.isEmpty()) {
                log.warn("DTO 유효성 검사 실패: {}", violations);
                throw new WebSocketException(INVALID_REQUEST_FORMAT);
            }

            return request;
        } catch (JsonProcessingException e) {
            throw new WebSocketException(INVALID_JSON_FORMAT);
        }
    }

    private void closeSessionWithError(WebSocketSession session, WebSocketErrorCode errorCode) {
        try {
            if (session.isOpen()) {
                session.close(errorCode.toCloseStatus());
            }
        } catch (IOException e) {
            log.error("세션 종료 중 오류 발생", e);
        } finally {
            sessionManager.removeSession(session);
            log.debug("세션 삭제: {}", session.getId());
        }
    }
}
