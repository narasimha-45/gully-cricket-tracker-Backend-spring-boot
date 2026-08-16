package com.gullycricket.backend.stats.dto;

public record FieldingAndMiscStatsResponse(
        String playerId,
        String playerName,
        Integer totalCatches,
        Integer totalRunOuts,
        Integer totalStumpings,
        Integer totalMatchesPlayed,
        Integer manOfTheMatchAwards
) {
}
