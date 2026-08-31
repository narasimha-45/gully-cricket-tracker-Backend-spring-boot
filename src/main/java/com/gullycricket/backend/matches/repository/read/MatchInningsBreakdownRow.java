package com.gullycricket.backend.matches.repository.read;

/**
 * One regular (non-super-over) innings row for the season match list, keyed by
 * which raw team batted it — the service layer swaps this onto display teamA/teamB
 * the same way it swaps runs/wickets/balls in {@link MatchSummaryRow}.
 */
public record MatchInningsBreakdownRow(
        String matchId,
        String teamId,
        int teamInningsNumber,
        int runs,
        int wickets,
        int balls,
        boolean completed,
        boolean followOn,
        String completionReason
) {
}
