package com.gullycricket.backend.stats.dto;

public record PlayerComparisonSideDto(
        String playerId,
        String playerName,
        Integer matchesPlayed,
        Integer matchesWon,
        Double winPercentage,
        Integer playerOfTheMatchAwards,
        BattingStatsResponse batting,
        BowlingStatsResponse bowling,
        FieldingAndMiscStatsResponse fielding
) {}
