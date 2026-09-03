package com.gullycricket.backend.matches.live.dto;

import java.util.List;

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
        Integer testInningsPerTeam,
        String tossWinner,
        String tossDecision,
        int currentInningsIndex,
        List<LiveInningsScoreDto> innings,
        long revision,
        long updatedAt
) {
}
