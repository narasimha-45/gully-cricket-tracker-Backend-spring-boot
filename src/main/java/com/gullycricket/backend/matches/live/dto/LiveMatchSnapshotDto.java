package com.gullycricket.backend.matches.live.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record LiveMatchSnapshotDto(
        String matchId,
        String seasonId,
        long revision,
        JsonNode match,
        long updatedAt
) {
}
