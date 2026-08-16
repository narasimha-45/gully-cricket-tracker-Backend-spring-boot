package com.gullycricket.backend.stats.dto;

public record TeamStatsForPlayerDto(
        String teamId,
        String teamName,

        Integer matchesPlayed,
        Integer matchesWon,
        Double winPercentage,

        Integer inningsBatted,
        Integer totalRuns,
        Double battingAverage,
        Double strikeRate,
        Integer highestScore,

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
