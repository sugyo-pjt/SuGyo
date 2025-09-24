package com.sugyo.domain.game.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sugyo.domain.game.domain.GameActionType;
import com.sugyo.domain.game.domain.GameSessionContext;
import com.sugyo.domain.game.domain.PlayValidationGroup;
import com.sugyo.domain.game.dto.request.GameActionRequest;
import com.sugyo.domain.game.entity.FrameCoordinates;
import com.sugyo.domain.game.exception.WebSocketErrorCode;
import com.sugyo.domain.game.exception.WebSocketException;
import com.sugyo.domain.game.repository.FrameCoordinatesRepository;
import com.sugyo.domain.game.service.FrameCoordinatesService;
import com.sugyo.domain.game.service.WebSocketGameService;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.sugyo.domain.game.exception.WebSocketErrorCode.INTERNAL_SERVER_ERROR;
import static com.sugyo.domain.game.exception.WebSocketErrorCode.INVALID_JSON_FORMAT;
import static com.sugyo.domain.game.exception.WebSocketErrorCode.INVALID_MUSIC_ID;
import static com.sugyo.domain.game.exception.WebSocketErrorCode.INVALID_REQUEST_FORMAT;
import static com.sugyo.domain.game.exception.WebSocketErrorCode.INVALID_URI_FORMAT;
import static com.sugyo.domain.game.exception.WebSocketErrorCode.SESSION_NOT_FOUND;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final WebSocketGameService gameService;
    private final FrameCoordinatesRepository frameCoordinatesRepository;

    private final Map<WebSocketSession, GameSessionContext> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        try {
            Long musicId = extractMusicIdFromUri(session.getUri());
            String userId = extractUserIdFromSession(session);

            FrameCoordinates frameCoordinates = frameCoordinatesRepository.findTop1ByMusicIdOrderByTimePassedDesc(musicId)
                    .orElseThrow(() -> new WebSocketException(INVALID_MUSIC_ID));

            double lastNoteTimestamp=frameCoordinates.getTimePassed();

            GameSessionContext context = new GameSessionContext(userId, musicId, session, lastNoteTimestamp);
            sessions.put(session, context);
            log.info("세션 시작: SessionId={}, UserId={}, MusicId={}", session.getId(), userId, musicId);
        } catch (IllegalArgumentException e) {
            closeSessionWithError(session, INVALID_MUSIC_ID);
        }
    }

    private Long extractMusicIdFromUri(URI uri) {
        String path = uri.getPath();
        try {
            return Long.parseLong(path.substring(path.lastIndexOf('/') + 1));
        } catch (NumberFormatException e) {
            throw new WebSocketException(INVALID_URI_FORMAT);
        }
    }

    private String extractUserIdFromSession(WebSocketSession session) {
        String userId = (String) session.getAttributes().get("userId");
        if (userId == null) {
            throw new WebSocketException(SESSION_NOT_FOUND);
        }
        return userId;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            GameSessionContext context = sessions.get(session);
            if (context == null) {
                throw new WebSocketException(SESSION_NOT_FOUND);
            }
            GameActionRequest request = parseAndValidate(message);

            switch (request.type()) {
                case PLAY -> gameService.processPlay(context, request);
                case PAUSE -> gameService.pauseGame(context);
                case RESUME -> gameService.resumeGame(context);
            }
        } catch (WebSocketException e) {
            log.warn("WebSocket 예외 발생: Code={}, Message={}", e.getErrorCode().getCode(), e.getMessage());
            closeSessionWithError(session, e.getErrorCode());
        } catch (Exception e) {
            log.error("예측하지 못한 서버 오류 발생: SessionId={}", session.getId(), e);
            closeSessionWithError(session, INTERNAL_SERVER_ERROR);
        }
    }

    private GameActionRequest parseAndValidate(TextMessage message) {
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
            sessions.remove(session);
        }
    }
}
