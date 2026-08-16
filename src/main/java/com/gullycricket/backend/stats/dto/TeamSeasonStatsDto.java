package com.gullycricket.backend.stats.dto;

public record TeamSeasonStatsDto(
        String seasonId,
        String seasonName,

        Integer matchesPlayed,
        Integer matchesWon,
        Integer matchesLost,
        Integer matchesTied,
        Integer matchesNoResult,
        Double winPercentage,

        Integer timesBattedFirst,
        Integer timesWonBattingFirst,
        Integer timesBattedSecond,
        Integer timesWonChasing,

        Integer highestTeamScore,
        Integer lowestTotalDefended,
        Integer highestTotalChased,
        Integer lowestTeamScore,

        Integer totalRunsScored,
        Integer totalRunsConceded
) {
}
