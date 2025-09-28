package com.sugyo.domain.game.controller;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sugyo.domain.game.domain.GameActionType;
import com.sugyo.domain.game.domain.GameSessionContext;
import com.sugyo.domain.game.dto.request.GameActionRequest;
import com.sugyo.domain.game.entity.FrameCoordinates;
import com.sugyo.domain.game.exception.WebSocketErrorCode;
import com.sugyo.domain.game.repository.FrameCoordinatesRepository;
import com.sugyo.domain.game.service.WebSocketGameService;
import com.sugyo.domain.game.service.WebSocketSessionManager;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameWebSocketHandlerTest {

    // @InjectMocks: @Mock 또는 @Spy 객체를 주입받을 대상
    @InjectMocks
    private GameWebSocketHandler gameWebSocketHandler;

    // --- Mock 객체들 ---
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private Validator validator;
    @Mock
    private WebSocketGameService gameService;
    @Mock
    private WebSocketSession session;
    @Mock
    private WebSocketSessionManager sessionManager;

    // @Spy: 실제 객체를 사용하되, 일부 메서드의 동작을 변경하고 싶을 때 사용
    // 여기서는 실제 ConcurrentHashMap을 사용해야 세션 추가/제거를 테스트할 수 있음
    @Spy
    private Map<WebSocketSession, GameSessionContext> sessions = new ConcurrentHashMap<>();

    @Nested
    @DisplayName("afterConnectionEstablished 메서드 테스트")
    class ConnectionTests {

        @Test
        @DisplayName("정상적인 연결 시 세션이 생성되고 SessionManager에 추가된다")
        void givenValidConnection_whenConnectionEstablished_thenAddSession() throws Exception {
            // given (준비)
            // 1. Mock WebSocketSession이 반환할 가짜 데이터 설정
            when(session.getUri()).thenReturn(new URI("ws://localhost/play/hard/123"));
            when(session.getAttributes()).thenReturn(Map.of("userId", "user-test-id"));
            when(session.getId()).thenReturn("session-test-id");

            // 2. gameService.initializeGameSession이 반환할 가짜 GameSessionContext 설정
            GameSessionContext mockContext = mock(GameSessionContext.class);
            when(gameService.initializeGameSession("user-test-id", 123L, session))
                    .thenReturn(mockContext);

            // when (실행)
            gameWebSocketHandler.afterConnectionEstablished(session);

            // then (검증)
            // 1. gameService.initializeGameSession이 올바른 인자들로 호출되었는지 검증
            verify(gameService, times(1)).initializeGameSession("user-test-id", 123L, session);

            // 2. sessionManager.addSession이 생성된 context와 함께 호출되었는지 검증
            verify(sessionManager, times(1)).addSession(session, mockContext);

            // 3. 에러로 인해 세션이 닫히는 일이 없었는지 검증
            verify(session, never()).close(any());
        }

        @Test
        @DisplayName("URI가 잘못되면 세션을 등록하지 않고, 정리 후 닫는다")
        void givenInvalidUri_whenConnectionEstablished_thenCleanupAndCloseSession() throws Exception {
            // given
            when(session.getUri()).thenReturn(new URI("ws://localhost/play/hard/invalid-id"));
            when(session.getId()).thenReturn("session-test-id");
            when(session.isOpen()).thenReturn(true);

            // when
            gameWebSocketHandler.afterConnectionEstablished(session);

            // then
            // 1. 서비스와는 상호작용이 없었음을 검증
            verifyNoInteractions(gameService);
            verify(sessionManager, never()).addSession(any(), any());

            // 2. ArgumentCaptor를 사용하여 close() 메서드에 전달된 CloseStatus 인자를 캡처
            ArgumentCaptor<CloseStatus> captor = ArgumentCaptor.forClass(CloseStatus.class);

            // 3. session.close()가 호출되었는지 검증하고, 호출 시 사용된 인자를 캡처
            verify(session, times(1)).close(captor.capture());

            // 4. 캡처된 인자의 내용을 검증
            CloseStatus capturedStatus = captor.getValue();
            assertThat(capturedStatus.getCode()).isEqualTo(WebSocketErrorCode.INVALID_URI_FORMAT.getCloseCode());
            assertThat(capturedStatus.getReason()).contains(WebSocketErrorCode.INVALID_URI_FORMAT.getMessage());

            // 5. 오류 처리 과정에서 세션이 '제거'되었는지 검증
            verify(sessionManager, times(1)).removeSession(session);
        }
    }

    @Nested
    @DisplayName("handleTextMessage 메서드 테스트")
    class MessageHandlingTests {

        @BeforeEach
        void setupSession() {
            // 모든 메시지 핸들링 테스트 전에 세션이 이미 저장되어 있다고 가정
//            GameSessionContext context = new GameSessionContext("1", 1L, session, 100.0);
//            sessions.put(session, context);
        }

        @Test
        @DisplayName("PLAY 타입 메시지 수신 시 gameService.processPlay를 호출한다")
        void givenPlayMessage_whenHandleTextMessage_thenCallProcessPlay() throws Exception {
            // given
            String playPayload = "{\"type\":\"PLAY\"}";
            GameActionRequest playRequest = new GameActionRequest(GameActionType.PLAY, 50.0, List.of());
            when(objectMapper.readValue(playPayload, GameActionRequest.class)).thenReturn(playRequest);
            // 유효성 검사는 통과했다고 가정
            when(validator.validate(any(), any(Class.class))).thenReturn(Collections.emptySet());

            // when
            gameWebSocketHandler.handleTextMessage(session, new TextMessage(playPayload));

            // then
            // gameService.processPlay가 정확한 인자와 함께 호출되었는지 검증
            verify(gameService, times(1)).processPlay(any(GameSessionContext.class), eq(playRequest));
            verify(gameService, never()).pauseGame(any());
            verify(gameService, never()).resumeGame(any());
        }

        @Test
        @DisplayName("PAUSE 타입 메시지 수신 시 gameService.pauseGame을 호출한다")
        void givenPauseMessage_whenHandleTextMessage_thenCallPauseGame() throws Exception {
            // given
            String pausePayload = "{\"type\":\"PAUSE\"}";
            GameActionRequest pauseRequest = new GameActionRequest(GameActionType.PAUSE, (double) 0, null);
            when(objectMapper.readValue(pausePayload, GameActionRequest.class)).thenReturn(pauseRequest);
            when(validator.validate(any())).thenReturn(Collections.emptySet());

            // when
            gameWebSocketHandler.handleTextMessage(session, new TextMessage(pausePayload));

            // then
            verify(gameService, times(1)).pauseGame(any(GameSessionContext.class));
            verify(gameService, never()).processPlay(any(), any());
        }
    }

    @Nested
    @DisplayName("afterConnectionClosed 메서드 테스트")
    class ConnectionClosedTests {

        @Test
        @DisplayName("연결이 종료되면 SessionManager에서 해당 세션을 제거한다")
        void whenConnectionClosed_thenRemoveSession() {
            // given
            CloseStatus status = CloseStatus.NORMAL;

            // when
            gameWebSocketHandler.afterConnectionClosed(session, status);

            // then
            // sessionManager.removeSession이 해당 세션 객체와 함께 호출되었는지 검증
            verify(sessionManager, times(1)).removeSession(session);
        }
    }
}
