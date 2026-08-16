package com.gullycricket.backend.stats.DTOs;

import com.gullycricket.backend.stats.enums.MatchResult;

import java.time.LocalDateTime;

public record NotableMatchDto(
        String matchId,
        String seasonId,
        String seasonName,
        String opponentTeamId,
        String opponentTeamName,
        Integer teamScore,
        Integer teamWickets,
        Integer opponentScore,
        Integer opponentWickets,
        Integer totalOvers,
        boolean battingFirst,
        MatchResult result,
        LocalDateTime completedAt
) {
}
