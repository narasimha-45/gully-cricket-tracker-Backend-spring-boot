package com.gullycricket.backend.matches.live.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gullycricket.backend.matches.live.dto.LiveMatchSnapshotDto;
import com.gullycricket.backend.matches.live.service.LiveMatchStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class LiveMatchWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final LiveMatchStateService liveMatchStateService;
    private final Map<String, Set<WebSocketSession>> sessionsByMatch = new ConcurrentHashMap<>();
    private final Map<String, String> matchBySessionId = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String matchId = extractMatchId(session);
        if (matchId == null || matchId.isBlank()) {
            session.close(CloseStatus.BAD_DATA.withReason("matchId is required"));
            return;
        }

        sessionsByMatch
                .computeIfAbsent(matchId, ignored -> ConcurrentHashMap.newKeySet())
                .add(session);
        matchBySessionId.put(session.getId(), matchId);

        liveMatchStateService.get(matchId).ifPresent(snapshot ->
                sendSafely(session, envelope("SNAPSHOT", snapshot.revision(), "snapshot", snapshot.match()))
        );
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String connectedMatchId = matchBySessionId.get(session.getId());
        if (connectedMatchId == null) {
            sendError(session, "Live match session is not registered");
            return;
        }

        try {
            JsonNode payload = objectMapper.readTree(message.getPayload());
            String type = payload.path("type").asText("");
            String matchId = payload.path("matchId").asText("");
            if (!connectedMatchId.equals(matchId)) {
                throw new IllegalArgumentException("Message matchId does not match socket subscription");
            }

            switch (type) {
                case "SYNC" -> handleSync(session, payload);
                case "PATCH" -> handlePatch(session, payload);
                default -> sendError(session, "Unsupported live message type: " + type);
            }
        } catch (LiveMatchStateService.UnauthorizedLiveScorerException ex) {
            sendError(session, ex.getMessage());
        } catch (LiveMatchStateService.MissingLiveSnapshotException ex) {
            sendSafely(session, simpleEnvelope("RESYNC_REQUIRED", 0));
        } catch (Exception ex) {
            log.warn("Invalid live WebSocket message sessionId={}", session.getId(), ex);
            sendError(session, ex.getMessage() == null ? "Invalid live scoring message" : ex.getMessage());
        }
    }

    private void handleSync(WebSocketSession session, JsonNode payload) {
        String matchId = payload.path("matchId").asText();
        String seasonId = payload.path("seasonId").asText();
        String scorerToken = payload.path("scorerToken").asText();
        long revision = payload.path("revision").asLong();
        JsonNode snapshot = payload.get("snapshot");

        LiveMatchStateService.UpdateResult result = liveMatchStateService.sync(
                matchId,
                seasonId,
                scorerToken,
                revision,
                snapshot
        );
        if (result.applied()) {
            broadcastExcept(
                    matchId,
                    session,
                    envelope("SNAPSHOT", result.snapshot().revision(), "snapshot", result.snapshot().match())
            );
        }
    }

    private void handlePatch(WebSocketSession session, JsonNode payload) {
        String matchId = payload.path("matchId").asText();
        String seasonId = payload.path("seasonId").asText();
        String scorerToken = payload.path("scorerToken").asText();
        long revision = payload.path("revision").asLong();
        JsonNode patch = payload.get("patch");

        LiveMatchStateService.UpdateResult result = liveMatchStateService.applyPatch(
                matchId,
                seasonId,
                scorerToken,
                revision,
                patch
        );
        if (result.applied()) {
            broadcastExcept(
                    matchId,
                    session,
                    envelope("PATCH", result.snapshot().revision(), "patch", patch)
            );
        }
    }

    public void broadcastEnded(String matchId, LiveMatchSnapshotDto ended) {
        if (ended == null) return;
        broadcastAll(
                matchId,
                envelope("ENDED", ended.revision(), "snapshot", ended.match())
        );
    }


    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String matchId = matchBySessionId.remove(session.getId());
        if (matchId == null) return;
        Set<WebSocketSession> sessions = sessionsByMatch.get(matchId);
        if (sessions == null) return;
        sessions.remove(session);
        if (sessions.isEmpty()) {
            sessionsByMatch.remove(matchId, sessions);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.debug("Live WebSocket transport error sessionId={}", session.getId(), exception);
    }

    private String extractMatchId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) return null;
        return UriComponentsBuilder.fromUri(uri)
                .build()
                .getQueryParams()
                .getFirst("matchId");
    }

    private ObjectNode envelope(String type, long revision, String field, JsonNode value) {
        ObjectNode node = simpleEnvelope(type, revision);
        node.set(field, value == null ? objectMapper.nullNode() : value.deepCopy());
        return node;
    }

    private ObjectNode simpleEnvelope(String type, long revision) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", type);
        node.put("revision", revision);
        return node;
    }

    private void sendError(WebSocketSession session, String message) {
        ObjectNode error = simpleEnvelope("ERROR", 0);
        error.put("message", message == null ? "Live scoring error" : message);
        sendSafely(session, error);
    }

    private void broadcastAll(String matchId, JsonNode message) {
        Set<WebSocketSession> sessions = sessionsByMatch.get(matchId);
        if (sessions == null || sessions.isEmpty()) return;
        sessions.forEach(session -> sendSafely(session, message));
    }

    private void broadcastExcept(String matchId, WebSocketSession sender, JsonNode message) {
        Set<WebSocketSession> sessions = sessionsByMatch.get(matchId);
        if (sessions == null || sessions.isEmpty()) return;
        sessions.forEach(session -> {
            if (session.getId().equals(sender.getId())) return;
            sendSafely(session, message);
        });
    }

    private void sendSafely(WebSocketSession session, JsonNode message) {
        if (session == null || !session.isOpen()) return;
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            }
        } catch (IOException ex) {
            log.debug("Could not send live WebSocket message sessionId={}", session.getId(), ex);
        }
    }
}
