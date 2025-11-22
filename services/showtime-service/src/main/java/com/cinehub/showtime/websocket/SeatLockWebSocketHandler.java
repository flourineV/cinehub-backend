package com.cinehub.showtime.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeatLockWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    private final Map<UUID, CopyOnWriteArraySet<WebSocketSession>> showtimeSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String path = session.getUri().getPath();
        UUID showtimeId = extractShowtimeId(path);

        if (showtimeId != null) {
            showtimeSessions.computeIfAbsent(showtimeId, k -> new CopyOnWriteArraySet<>()).add(session);
            session.getAttributes().put("showtimeId", showtimeId);
            log.info("WebSocket connected: session={}, showtimeId={}", session.getId(), showtimeId);
        } else {
            log.warn("Invalid WebSocket path: {}", path);
            session.close(CloseStatus.BAD_DATA);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        UUID showtimeId = (UUID) session.getAttributes().get("showtimeId");
        if (showtimeId != null) {
            CopyOnWriteArraySet<WebSocketSession> sessions = showtimeSessions.get(showtimeId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    showtimeSessions.remove(showtimeId);
                }
            }
        }
        log.info("WebSocket disconnected: session={}, status={}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket error for session {}: {}", session.getId(), exception.getMessage());
        session.close(CloseStatus.SERVER_ERROR);
    }

    public void broadcastToShowtime(UUID showtimeId, Object message) {
        CopyOnWriteArraySet<WebSocketSession> sessions = showtimeSessions.get(showtimeId);
        if (sessions != null && !sessions.isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(message);
                TextMessage textMessage = new TextMessage(json);

                for (WebSocketSession session : sessions) {
                    if (session.isOpen()) {
                        session.sendMessage(textMessage);
                    }
                }
                log.debug("Broadcasted to {} sessions for showtime {}", sessions.size(), showtimeId);
            } catch (Exception e) {
                log.error("Error broadcasting to showtime {}: {}", showtimeId, e.getMessage());
            }
        }
    }

    private UUID extractShowtimeId(String path) {
        try {
            String[] parts = path.split("/");
            if (parts.length > 0) {
                String idPart = parts[parts.length - 1];
                return UUID.fromString(idPart);
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID in path: {}", path);
        }
        return null;
    }
}
