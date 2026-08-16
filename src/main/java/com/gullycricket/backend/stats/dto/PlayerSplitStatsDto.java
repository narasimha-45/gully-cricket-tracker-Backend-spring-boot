package com.gullycricket.backend.stats.dto;

public record PlayerSplitStatsDto(
        String splitKey,
        String splitLabel,

        Integer matchesPlayed,
        Integer matchesWon,

        Integer inningsBatted,
        Integer totalRuns,
        Integer totalBallsFaced,
        Double battingAverage,
        Double strikeRate,
        Integer highestScore,

        Integer inningsBowled,
        Integer totalWickets,
        Integer totalRunsConceded,
        Double economyRate,
        Double bowlingAverage
) {
}
