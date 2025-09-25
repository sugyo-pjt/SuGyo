package com.sugyo.domain.game.controller;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sugyo.domain.game.domain.GameActionType;
import com.sugyo.domain.game.domain.GameSessionContext;
import com.sugyo.domain.game.dto.request.GameActionRequest;
import com.sugyo.domain.game.entity.FrameCoordinates;
import com.sugyo.domain.game.repository.FrameCoordinatesRepository;
import com.sugyo.domain.game.service.WebSocketGameService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
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
    private FrameCoordinatesRepository frameCoordinatesRepository;
    @Mock
    private WebSocketSession session;

    // @Spy: 실제 객체를 사용하되, 일부 메서드의 동작을 변경하고 싶을 때 사용
    // 여기서는 실제 ConcurrentHashMap을 사용해야 세션 추가/제거를 테스트할 수 있음
    @Spy
    private Map<WebSocketSession, GameSessionContext> sessions = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() throws Exception {
        // Reflection을 사용해 private final 필드인 sessions에 @Spy 객체를 주입
        // 테스트 대상 클래스의 내부 구현에 의존하므로 좋은 방식은 아니지만,
        // private final 필드를 테스트해야 할 때 불가피하게 사용될 수 있음
        java.lang.reflect.Field field = GameWebSocketHandler.class.getDeclaredField("sessions");
        field.setAccessible(true);
        field.set(gameWebSocketHandler, sessions);
    }

    @Nested
    @DisplayName("afterConnectionEstablished 메서드 테스트")
    class ConnectionTests {

        @Test
        @DisplayName("정상적인 연결 시 세션이 생성되고 저장된다")
        void givenValidConnection_whenAfterConnectionEstablished_thenSessionIsStored() throws Exception {
            // given
            when(session.getUri()).thenReturn(URI.create("ws://localhost:8080/play/1"));
            when(session.getAttributes()).thenReturn(Map.of("userId", "test-user"));
            FrameCoordinates mockCoords = mock(FrameCoordinates.class);
            when(mockCoords.getTimePassed()).thenReturn(120.5);
            when(frameCoordinatesRepository.findTop1ByMusicIdOrderByTimePassedDesc(1L))
                    .thenReturn(Optional.of(mockCoords));

            // when
            gameWebSocketHandler.afterConnectionEstablished(session);

            // then
            assertThat(sessions).hasSize(1);
            assertThat(sessions.get(session).getUserId()).isEqualTo("test-user");
            assertThat(sessions.get(session).getMusicId()).isEqualTo(1L);
            assertThat(sessions.get(session).getLastNoteTimestamp()).isEqualTo(120.5);
        }

        @Test
        @DisplayName("MusicId가 유효하지 않으면 예외가 발생하고 세션이 닫힌다")
        void givenInvalidMusicId_whenAfterConnectionEstablished_thenClosesSession() throws Exception {
            // given
            when(session.getUri()).thenReturn(URI.create("ws://localhost:8080/play/invalid"));

            // when
            gameWebSocketHandler.afterConnectionEstablished(session);

            // then
            // closeSessionWithError가 호출되었는지 검증
            verify(session, times(1)).close(any());
            assertThat(sessions).isEmpty();
        }
    }

    @Nested
    @DisplayName("handleTextMessage 메서드 테스트")
    class MessageHandlingTests {

        @BeforeEach
        void setupSession() {
            // 모든 메시지 핸들링 테스트 전에 세션이 이미 저장되어 있다고 가정
            GameSessionContext context = new GameSessionContext("1", 1L, session, 100.0);
            sessions.put(session, context);
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
}
