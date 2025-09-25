package com.sugyo.domain.game.service;

import com.sugyo.domain.game.domain.GameSessionContext;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Component
public class WebSocketSessionManager {

    private final Map<WebSocketSession, GameSessionContext> sessions = new ConcurrentHashMap<>();

    public void addSession(WebSocketSession session, GameSessionContext context) {
        sessions.put(session, context);
    }

    public void removeSession(WebSocketSession session) {
        sessions.remove(session);
    }

    public GameSessionContext getContext(WebSocketSession session) {
        return sessions.get(session);
    }
}
