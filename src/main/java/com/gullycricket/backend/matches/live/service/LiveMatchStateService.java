package com.gullycricket.backend.matches.live.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gullycricket.backend.matches.live.dto.LiveMatchSnapshotDto;
import com.gullycricket.backend.matches.live.dto.LiveMatchSummaryDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class LiveMatchStateService {

    private final Map<String, LiveMatchEntry> matches = new ConcurrentHashMap<>();
    private final Clock clock = Clock.systemUTC();
    private final long ttlMs;

    public LiveMatchStateService(
            @Value("${app.live-scoring.state-ttl-ms:43200000}") long ttlMs) {
        this.ttlMs = Math.max(60_000L, ttlMs);
    }

    public synchronized UpdateResult sync(
            String matchId,
            String seasonId,
            String scorerToken,
            long revision,
            JsonNode snapshot) {
        requireIdentity(matchId, seasonId, scorerToken);
        if (snapshot == null || !snapshot.isObject()) {
            throw new IllegalArgumentException("Live snapshot is required");
        }
        validateSnapshotIdentity(matchId, seasonId, snapshot);

        cleanupExpired();
        LiveMatchEntry existing = matches.get(matchId);
        if (existing != null) {
            verifyScorer(existing, scorerToken);
            if (!existing.seasonId().equals(seasonId)) {
                throw new IllegalArgumentException("seasonId does not match the registered live match");
            }
        }

        long effectiveRevision = existing == null
                ? Math.max(1L, revision)
                : Math.max(existing.revision() + 1L, revision);
        long now = clock.millis();
        ObjectNode safeSnapshot = ((ObjectNode) snapshot).deepCopy();
        safeSnapshot.remove("liveScoring");
        stripUndoHistory(safeSnapshot);

        LiveMatchEntry next = new LiveMatchEntry(
                matchId,
                seasonId,
                scorerToken,
                effectiveRevision,
                safeSnapshot,
                now
        );
        matches.put(matchId, next);
        return new UpdateResult(true, toSnapshot(next));
    }

    public synchronized UpdateResult applyPatch(
            String matchId,
            String seasonId,
            String scorerToken,
            long revision,
            JsonNode patch) {
        requireIdentity(matchId, seasonId, scorerToken);
        if (patch == null || !patch.isObject()) {
            throw new IllegalArgumentException("Live patch is required");
        }

        cleanupExpired();
        LiveMatchEntry existing = matches.get(matchId);
        if (existing == null) {
            throw new MissingLiveSnapshotException("Full live sync required before patches");
        }
        if (!existing.seasonId().equals(seasonId)) {
            throw new IllegalArgumentException("seasonId does not match the registered live match");
        }
        verifyScorer(existing, scorerToken);
        long effectiveRevision = Math.max(existing.revision() + 1L, revision);

        ObjectNode updated = existing.snapshot().deepCopy();
        applyPatch(updated, (ObjectNode) patch);
        stripUndoHistory(updated);

        LiveMatchEntry next = new LiveMatchEntry(
                matchId,
                seasonId,
                scorerToken,
                effectiveRevision,
                updated,
                clock.millis()
        );
        matches.put(matchId, next);
        return new UpdateResult(true, toSnapshot(next));
    }

    public synchronized Optional<LiveMatchSnapshotDto> get(String matchId) {
        cleanupExpired();
        LiveMatchEntry entry = matches.get(matchId);
        return entry == null ? Optional.empty() : Optional.of(toSnapshot(entry));
    }

    public synchronized List<LiveMatchSummaryDto> listBySeason(String seasonId) {
        cleanupExpired();
        List<LiveMatchSummaryDto> result = new ArrayList<>();
        for (LiveMatchEntry entry : matches.values()) {
            if (!entry.seasonId().equals(seasonId)) {
                continue;
            }
            JsonNode snapshot = entry.snapshot();
            if (!"LIVE".equalsIgnoreCase(snapshot.path("status").asText())) {
                continue;
            }
            result.add(toSummary(entry));
        }
        result.sort(Comparator.comparingLong(LiveMatchSummaryDto::updatedAt).reversed());
        return result;
    }

    public synchronized Optional<LiveMatchSnapshotDto> endAndRemove(
            String matchId,
            String scorerToken,
            JsonNode finalSnapshot) {
        cleanupExpired();
        LiveMatchEntry existing = matches.get(matchId);
        if (existing == null) {
            return Optional.empty();
        }

        verifyScorer(existing, scorerToken);
        ObjectNode snapshot = finalSnapshot != null && finalSnapshot.isObject()
                ? ((ObjectNode) finalSnapshot).deepCopy()
                : existing.snapshot().deepCopy();
        validateSnapshotIdentity(matchId, existing.seasonId(), snapshot);
        snapshot.remove("liveScoring");
        stripUndoHistory(snapshot);

        LiveMatchSnapshotDto result = new LiveMatchSnapshotDto(
                matchId,
                existing.seasonId(),
                existing.revision() + 1L,
                snapshot,
                clock.millis()
        );
        matches.remove(matchId);
        return Optional.of(result);
    }

    public synchronized boolean isAuthorizedScorer(String matchId, String scorerToken) {
        cleanupExpired();
        LiveMatchEntry entry = matches.get(matchId);
        return entry != null && tokenEquals(entry.scorerToken(), scorerToken);
    }

    private void applyPatch(ObjectNode snapshot, ObjectNode patch) {
        JsonNode topLevel = patch.get("topLevel");
        if (topLevel != null && topLevel.isObject()) {
            topLevel.fields().forEachRemaining(field ->
                    snapshot.set(field.getKey(), field.getValue().deepCopy())
            );
        }

        JsonNode live = patch.get("live");
        if (live != null && live.isObject()) {
            snapshot.set("live", live.deepCopy());
        }

        ArrayNode innings = ensureArray(snapshot, "innings");
        JsonNode inningsPatches = patch.get("innings");
        int requestedInningsCount = -1;
        if (inningsPatches != null && inningsPatches.isArray()) {
            requestedInningsCount = inningsPatches.size();
            for (JsonNode inningsPatch : inningsPatches) {
                int index = inningsPatch.path("inningsIndex").asInt(-1);
                JsonNode summary = inningsPatch.get("summary");
                if (index < 0 || summary == null || !summary.isObject()) {
                    continue;
                }
                ensureArraySize(innings, index + 1);
                JsonNode current = innings.get(index);
                ArrayNode currentBalls = current != null && current.path("ballByBall").isArray()
                        ? ((ArrayNode) current.path("ballByBall")).deepCopy()
                        : JsonNodeFactory.instance.arrayNode();
                ObjectNode replacement = ((ObjectNode) summary).deepCopy();
                replacement.set("ballByBall", currentBalls);
                innings.set(index, replacement);
            }
        }

        if (requestedInningsCount >= 0) {
            while (innings.size() > requestedInningsCount) {
                innings.remove(innings.size() - 1);
            }
        }

        JsonNode ballDelta = patch.get("ballDelta");
        if (ballDelta != null && ballDelta.isObject()) {
            int index = ballDelta.path("inningsIndex").asInt(-1);
            if (index >= 0) {
                ensureArraySize(innings, index + 1);
                ObjectNode target = innings.get(index) != null && innings.get(index).isObject()
                        ? (ObjectNode) innings.get(index)
                        : JsonNodeFactory.instance.objectNode();
                innings.set(index, target);
                ArrayNode balls = ensureArray(target, "ballByBall");
                int truncateTo = Math.max(0, Math.min(
                        ballDelta.path("truncateTo").asInt(balls.size()),
                        balls.size()
                ));
                while (balls.size() > truncateTo) {
                    balls.remove(balls.size() - 1);
                }
                JsonNode append = ballDelta.get("append");
                if (append != null && append.isArray()) {
                    append.forEach(ball -> balls.add(ball.deepCopy()));
                }
            }
        }
    }

    private LiveMatchSnapshotDto toSnapshot(LiveMatchEntry entry) {
        return new LiveMatchSnapshotDto(
                entry.matchId(),
                entry.seasonId(),
                entry.revision(),
                entry.snapshot().deepCopy(),
                entry.updatedAt()
        );
    }

    private LiveMatchSummaryDto toSummary(LiveMatchEntry entry) {
        JsonNode snapshot = entry.snapshot();
        JsonNode teams = snapshot.path("teams");
        int inningsIndex = snapshot.path("live").path("inningsIndex").asInt(0);
        JsonNode innings = snapshot.path("innings");
        JsonNode current = innings.isArray() && inningsIndex >= 0 && inningsIndex < innings.size()
                ? innings.get(inningsIndex)
                : null;

        return new LiveMatchSummaryDto(
                entry.matchId(),
                entry.seasonId(),
                teams.path("teamA").path("name").asText(""),
                teams.path("teamB").path("name").asText(""),
                current == null ? "" : current.path("battingTeam").asText(""),
                current == null ? 0 : current.path("totalRuns").asInt(0),
                current == null ? 0 : current.path("wickets").asInt(0),
                current == null ? 0 : current.path("balls").asInt(0),
                snapshot.path("matchType").asText(""),
                snapshot.get("totalOvers") == null || snapshot.get("totalOvers").isNull()
                        ? null
                        : snapshot.get("totalOvers").asInt(),
                entry.revision(),
                entry.updatedAt()
        );
    }

    private void cleanupExpired() {
        long cutoff = clock.millis() - ttlMs;
        matches.entrySet().removeIf(entry -> entry.getValue().updatedAt() < cutoff);
    }

    private void verifyScorer(LiveMatchEntry entry, String scorerToken) {
        if (!tokenEquals(entry.scorerToken(), scorerToken)) {
            throw new UnauthorizedLiveScorerException("Invalid scorer token for live match");
        }
    }

    private void requireIdentity(String matchId, String seasonId, String scorerToken) {
        if (matchId == null || matchId.isBlank()) {
            throw new IllegalArgumentException("matchId is required");
        }
        if (seasonId == null || seasonId.isBlank()) {
            throw new IllegalArgumentException("seasonId is required");
        }
        if (scorerToken == null || scorerToken.isBlank()) {
            throw new UnauthorizedLiveScorerException("scorerToken is required");
        }
    }

    private void validateSnapshotIdentity(String matchId, String seasonId, JsonNode snapshot) {
        String snapshotMatchId = snapshot.path("id").asText("");
        String snapshotSeasonId = snapshot.path("seasonId").asText("");
        if (!matchId.equals(snapshotMatchId)) {
            throw new IllegalArgumentException("Snapshot id does not match matchId");
        }
        if (!seasonId.equals(snapshotSeasonId)) {
            throw new IllegalArgumentException("Snapshot seasonId does not match seasonId");
        }
    }

    private void stripUndoHistory(ObjectNode snapshot) {
        JsonNode live = snapshot.get("live");
        if (live instanceof ObjectNode liveObject) {
            liveObject.remove("history");
        }
    }

    private boolean tokenEquals(String expected, String provided) {
        if (expected == null || provided == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8)
        );
    }

    private ArrayNode ensureArray(ObjectNode parent, String field) {
        JsonNode existing = parent.get(field);
        if (existing instanceof ArrayNode arrayNode) {
            return arrayNode;
        }
        ArrayNode created = parent.arrayNode();
        parent.set(field, created);
        return created;
    }

    private void ensureArraySize(ArrayNode array, int size) {
        while (array.size() < size) {
            array.addObject();
        }
    }

    private record LiveMatchEntry(
            String matchId,
            String seasonId,
            String scorerToken,
            long revision,
            ObjectNode snapshot,
            long updatedAt
    ) {
    }

    public record UpdateResult(boolean applied, LiveMatchSnapshotDto snapshot) {
    }

    public static class UnauthorizedLiveScorerException extends RuntimeException {
        public UnauthorizedLiveScorerException(String message) {
            super(message);
        }
    }

    public static class MissingLiveSnapshotException extends RuntimeException {
        public MissingLiveSnapshotException(String message) {
            super(message);
        }
    }
}
