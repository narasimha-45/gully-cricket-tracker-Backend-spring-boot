package com.gullycricket.backend.stats.dto;

public record TeamLeaderboardEntryDto(
        String teamId,
        String teamName,

        Integer matchesPlayed,
        Integer matchesWon,
        Integer matchesLost,
        Integer matchesTied,
        Integer matchesNoResult,
        Double winPercentage,

        Integer timesWonBattingFirst,
        Integer timesWonChasing,

        Integer highestTeamScore,
        Integer lowestTotalDefended,
        Integer highestTotalChased,

        Integer totalRunsScored,
        Integer totalRunsConceded
) {
}
