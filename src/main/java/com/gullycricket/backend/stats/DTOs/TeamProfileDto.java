package com.gullycricket.backend.stats.DTOs;

import java.util.List;

public record TeamProfileDto(
        String teamId,
        String teamName,

        Integer matchesPlayed,
        Integer matchesWon,
        Integer matchesLost,
        Integer matchesTied,
        Integer matchesNoResult,
        Double winPercentage,

        Integer timesBattedFirst,
        Integer timesWonBattingFirst,
        Double winPercentageBattingFirst,
        Integer timesBattedSecond,
        Integer timesWonChasing,
        Double winPercentageChasing,

        NotableMatchDto highestTeamScore,
        NotableMatchDto lowestTeamScore,
        NotableMatchDto lowestTotalDefended,
        NotableMatchDto highestTotalChased,

        Integer totalRunsScored,
        Integer totalRunsConceded,
        Double averageScore,

        List<NotableMatchDto> recentMatches,
        List<TeamSeasonStatsDto> bySeason
) {
}
