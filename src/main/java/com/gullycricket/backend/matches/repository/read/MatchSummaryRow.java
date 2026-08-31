package com.gullycricket.backend.matches.repository.read;

import com.gullycricket.backend.matches.entity.MatchStatus;

import java.time.LocalDateTime;

/** Compact match read model that intentionally excludes matches.match_data JSONB. */
public record MatchSummaryRow(
        String matchId,
        String seasonId,
        String seasonName,
        String teamAId,
        String teamAName,
        int teamARuns,
        int teamAWickets,
        int teamABalls,
        String teamBId,
        String teamBName,
        int teamBRuns,
        int teamBWickets,
        int teamBBalls,
        String battingFirstTeamId,
        String winnerTeamId,
        String winnerTeamName,
        boolean matchTied,
        boolean matchDrawn,
        boolean superOver,
        String wonBy,
        LocalDateTime completedAt,
        MatchStatus status,
        Integer totalOvers
) {
}
