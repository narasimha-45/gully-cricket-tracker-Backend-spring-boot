package com.gullycricket.backend.matches.repository.read;

import com.gullycricket.backend.matches.entity.MatchStatus;
import com.gullycricket.backend.matches.entity.MatchType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Compact match read model that intentionally excludes matches.match_data JSONB.
 *
 * <p>{@code teamARuns}/{@code teamAWickets}/{@code teamABalls} (and the teamB equivalents)
 * are the SUM across every innings that team has batted, which is meaningful on its own
 * for a single-innings (OVERS) match but is a combined total for a Test match — it is
 * intentionally kept around for aggregate stats (e.g. "highest team total in a season")
 * where an all-innings total is the right number. Anything that needs to *display* a
 * Test match score (e.g. "286 & 177-7") must use {@link #teamAInnings()} /
 * {@link #teamBInnings()} instead, which preserve each innings separately.
 */
public record MatchSummaryRow(
        String matchId,
        String seasonId,
        String seasonName,
        String teamAId,
        String teamAName,
        int teamARuns,
        int teamAWickets,
        int teamABalls,
        List<InningsScoreRow> teamAInnings,
        String teamBId,
        String teamBName,
        int teamBRuns,
        int teamBWickets,
        int teamBBalls,
        List<InningsScoreRow> teamBInnings,
        String battingFirstTeamId,
        String winnerTeamId,
        String winnerTeamName,
        boolean matchTied,
        boolean matchDrawn,
        boolean superOver,
        String wonBy,
        LocalDateTime completedAt,
        MatchStatus status,
        MatchType matchType,
        Integer totalOvers
) {
}
