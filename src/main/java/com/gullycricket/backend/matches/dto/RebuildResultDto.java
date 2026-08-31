package com.gullycricket.backend.matches.dto;

public record RebuildResultDto(
        String scope,
        String matchId,
        String seasonId,
        int matchesReplayed,
        String message
) {
}
