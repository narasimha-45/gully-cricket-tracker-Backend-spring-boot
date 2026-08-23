package com.gullycricket.backend.stats.dto;

import java.util.List;

public record PlayerProfileDto(
        String playerId,
        String playerName,

        Integer totalMatchesPlayed,
        Integer totalMatchesWon,
        Double winPercentage,
        Integer playerOfTheMatchAwards,

        BattingStatsResponse overallBatting,
        BowlingStatsResponse overallBowling,
        FieldingAndMiscStatsResponse overallFielding,

        List<RecentPerformanceDto> recentPerformances,

        List<BattingPositionStatsDto> byBattingPosition,

        List<PlayerSplitStatsDto> byInnings,

        List<PlayerSplitStatsDto> byMatchResult,

        List<SeasonPlayerStatsDto> bySeason,

        List<TeamStatsForPlayerDto> byTeam
) {
}
