package com.gullycricket.backend.matches.repository.read;

/**
 * A single team-innings score line (e.g. Test cricket has up to two of these per team).
 * {@code inningsNumber} is the team's own innings count (1 or 2), not the absolute
 * over-the-whole-match sequence number.
 */
public record InningsScoreRow(
        Integer inningsNumber,
        int runs,
        int wickets,
        int balls,
        boolean completed
) {
}
