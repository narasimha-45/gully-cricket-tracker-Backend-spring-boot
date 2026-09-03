package com.gullycricket.backend.matches.live.controller;

import com.gullycricket.backend.common.exception.ResourceNotFoundException;
import com.gullycricket.backend.matches.live.dto.LiveMatchSnapshotDto;
import com.gullycricket.backend.matches.live.dto.LiveMatchSummaryDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.gullycricket.backend.matches.live.service.LiveMatchStateService;
import com.gullycricket.backend.matches.live.websocket.LiveMatchWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/live-matches")
@RequiredArgsConstructor
public class LiveMatchController {

    private final LiveMatchStateService liveMatchStateService;
    private final LiveMatchWebSocketHandler liveMatchWebSocketHandler;

    @GetMapping
    public ResponseEntity<List<LiveMatchSummaryDto>> listLiveMatches(
            @RequestParam String seasonId) {
        return ResponseEntity.ok(liveMatchStateService.listBySeason(seasonId));
    }

    @GetMapping("/{matchId}")
    public ResponseEntity<LiveMatchSnapshotDto> getLiveMatch(
            @PathVariable String matchId) {
        return ResponseEntity.ok(
                liveMatchStateService.get(matchId)
                        .orElseThrow(() -> new ResourceNotFoundException("Live match not found: " + matchId))
        );
    }

    @PostMapping("/{matchId}/end")
    public ResponseEntity<Void> endLiveMatch(
            @PathVariable String matchId,
            @RequestHeader("X-Live-Scorer-Token") String scorerToken,
            @RequestBody(required = false) JsonNode finalSnapshot) {
        liveMatchStateService.endAndRemove(matchId, scorerToken, finalSnapshot)
                .ifPresent(ended -> liveMatchWebSocketHandler.broadcastEnded(matchId, ended));
        return ResponseEntity.noContent().build();
    }
}
