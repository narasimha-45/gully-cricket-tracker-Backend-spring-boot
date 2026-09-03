package com.gullycricket.backend.matches.live.dto;

public record LiveMatchSummaryDto(
        String matchId,
        String seasonId,
        String teamA,
        String teamB,
        String battingTeam,
        int runs,
        int wickets,
        int balls,
        String matchType,
        Integer totalOvers,
        long revision,
        long updatedAt
) {
}
