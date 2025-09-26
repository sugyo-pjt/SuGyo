package com.sugyo.domain.game.service;

import com.sugyo.domain.game.domain.GameSessionContext;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionManager {

    private final Map<String, GameSessionContext> sessions = new ConcurrentHashMap<>();

    public void addSession(WebSocketSession session, GameSessionContext context) {
        sessions.put(session.getId(), context);
    }

    public void removeSession(WebSocketSession session) {
        sessions.remove(session.getId());
    }

    public GameSessionContext getContext(WebSocketSession session) {
        return sessions.get(session.getId());
    }

    public Map<String, GameSessionContext> getSessions() {
        return new ConcurrentHashMap<>(sessions);
    }
}
