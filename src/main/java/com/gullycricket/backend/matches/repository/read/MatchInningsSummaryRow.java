package com.gullycricket.backend.matches.repository.read;

/** Lightweight innings row used by season match cards without loading match_data JSONB. */
public record MatchInningsSummaryRow(
        String matchId,
        int sequenceNumber,
        int teamInningsNumber,
        String battingTeamName,
        int runs,
        int wickets,
        int balls,
        boolean superOver,
        boolean completed
) {
}
