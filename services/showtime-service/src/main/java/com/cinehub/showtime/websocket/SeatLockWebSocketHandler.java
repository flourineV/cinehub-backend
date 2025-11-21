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

    // Map: showtimeId -> Set of WebSocket sessions
    private final Map<UUID, CopyOnWriteArraySet<WebSocketSession>> showtimeSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String uri = session.getUri().toString();
        UUID showtimeId = extractShowtimeId(uri);

        if (showtimeId != null) {
            showtimeSessions.computeIfAbsent(showtimeId, k -> new CopyOnWriteArraySet<>()).add(session);
            session.getAttributes().put("showtimeId", showtimeId);
            log.info("WebSocket connected: session={}, showtimeId={}", session.getId(), showtimeId);
        } else {
            log.warn("WebSocket connection without showtimeId: {}", uri);
            session.close(CloseStatus.BAD_DATA);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Handle incoming messages if needed (currently read-only for seat lock
        // updates)
        log.debug("Received message from session {}: {}", session.getId(), message.getPayload());
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

    /**
     * Broadcast message to all sessions subscribed to a showtime
     */
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

    /**
     * Extract showtimeId from WebSocket URI: /ws/showtime/{showtimeId}
     */
    private UUID extractShowtimeId(String uri) {
        try {
            String[] parts = uri.split("/");
            for (int i = 0; i < parts.length - 1; i++) {
                if ("showtime".equals(parts[i])) {
                    return UUID.fromString(parts[i + 1]);
                }
            }
        } catch (Exception e) {
            log.error("Failed to extract showtimeId from URI: {}", uri, e);
        }
        return null;
    }
}
