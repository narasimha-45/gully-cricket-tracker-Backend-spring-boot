package com.gullycricket.backend.stats.dto;

public record SeasonPlayerStatsDto(
        String seasonId,
        String seasonName,

        Integer matchesPlayed,
        Integer matchesWon,

        Integer inningsBatted,
        Integer totalRuns,
        Double battingAverage,
        Double strikeRate,
        Integer highestScore,
        Integer fifties,
        Integer hundreds,

        Integer inningsBowled,
        Integer totalWickets,
        Double economyRate,
        Double bowlingAverage,

        Integer catches,
        Integer runOuts,
        Integer stumpings,
        Integer manOfTheMatchAwards
) {
}
