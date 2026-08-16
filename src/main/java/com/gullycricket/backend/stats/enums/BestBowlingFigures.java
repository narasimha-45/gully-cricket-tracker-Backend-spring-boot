package com.gullycricket.backend.stats.enums;

/**
 * Best single-innings bowling figures. Kept in the existing package to avoid
 * changing the public response type name used by the frontend.
 */
public record BestBowlingFigures(
        int wickets,
        int runsConceded,
        int ballsBowled
) {}
