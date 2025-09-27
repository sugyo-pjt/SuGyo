package com.sugyo.domain.game.service;

import com.sugyo.domain.game.domain.GameSessionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSocketSessionManagerTest {

    private WebSocketSessionManager sessionManager;

    @BeforeEach
    void setUp() {
        sessionManager = new WebSocketSessionManager();
    }

    @Nested
    @DisplayName("addSession 메서드 테스트")
    class AddSessionTests {

        @Test
        @DisplayName("세션을 추가하면 내부 맵의 사이즈가 1 증가한다")
        void whenAddSession_thenMapSizeIncreases() {
            // given (준비)
            WebSocketSession session = mock(WebSocketSession.class);
            when(session.getId()).thenReturn("session-id-1");
            GameSessionContext context = mock(GameSessionContext.class);

            // when (실행)
            sessionManager.addSession(session, context);

            // then (검증)
            Map<String, GameSessionContext> sessions = sessionManager.getSessions();
            assertThat(sessions.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("추가된 세션은 올바른 키와 값으로 저장된다")
        void whenAddSession_thenStoredWithCorrectKeyAndValue() {
            // given
            String sessionId = "session-id-2";
            WebSocketSession session = mock(WebSocketSession.class);
            when(session.getId()).thenReturn(sessionId);
            GameSessionContext context = mock(GameSessionContext.class);

            // when
            sessionManager.addSession(session, context);

            // then
            Map<String, GameSessionContext> sessions = sessionManager.getSessions();
            // 1. 해당 ID의 키가 존재하는지 확인
            assertThat(sessions).containsKey(sessionId);
            // 2. 해당 키로 조회한 값이 전달한 context 객체와 동일한지 확인
            assertThat(sessions.get(sessionId)).isSameAs(context);
        }
    }

    @Nested
    @DisplayName("removeSession 메서드 테스트")
    class RemoveSessionTests {

        @Test
        @DisplayName("세션을 제거하면 내부 맵에서 해당 항목이 사라진다")
        void whenRemoveSession_thenEntryIsRemovedFromMap() {
            // given
            String sessionId = "session-id-3";
            WebSocketSession session = mock(WebSocketSession.class);
            when(session.getId()).thenReturn(sessionId);
            GameSessionContext context = mock(GameSessionContext.class);

            // 먼저 세션을 추가
            sessionManager.addSession(session, context);
            assertThat(sessionManager.getSessions()).hasSize(1); // 추가 확인

            // when
            sessionManager.removeSession(session);

            // then
            Map<String, GameSessionContext> sessions = sessionManager.getSessions();
            // 1. 맵이 비어있는지 확인
            assertThat(sessions).isEmpty();
            // 2. 더 이상 해당 키가 존재하지 않는지 확인
            assertThat(sessions).doesNotContainKey(sessionId);
        }

        @Test
        @DisplayName("존재하지 않는 세션을 제거해도 오류가 발생하지 않는다")
        void whenRemoveNonExistentSession_thenNoErrorOccurs() {
            // given
            WebSocketSession nonExistentSession = mock(WebSocketSession.class);
            when(nonExistentSession.getId()).thenReturn("non-existent-id");

            // when & then
            // 예외가 발생하지 않고 정상적으로 코드가 실행되는지 확인하는 것 자체가 테스트
            sessionManager.removeSession(nonExistentSession);

            // 맵이 여전히 비어있는지 확인
            assertThat(sessionManager.getSessions()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getContext 메서드 테스트")
    class GetContextTests {

        @Test
        @DisplayName("존재하는 세션 ID로 조회 시 올바른 컨텍스트를 반환한다")
        void givenExistingSession_whenGetContext_thenReturnCorrectContext() {
            // given
            String sessionId = "session-id-4";
            WebSocketSession session = mock(WebSocketSession.class);
            when(session.getId()).thenReturn(sessionId);
            GameSessionContext expectedContext = mock(GameSessionContext.class);

            sessionManager.addSession(session, expectedContext);

            // when
            GameSessionContext actualContext = sessionManager.getContext(session);

            // then
            assertThat(actualContext).isNotNull();
            assertThat(actualContext).isSameAs(expectedContext);
        }

        @Test
        @DisplayName("존재하지 않는 세션 ID로 조회 시 null을 반환한다")
        void givenNonExistentSession_whenGetContext_thenReturnNull() {
            // given
            WebSocketSession nonExistentSession = mock(WebSocketSession.class);
            when(nonExistentSession.getId()).thenReturn("non-existent-id");

            // when
            GameSessionContext actualContext = sessionManager.getContext(nonExistentSession);

            // then
            assertThat(actualContext).isNull();
        }
    }
}
