package com.gullycricket.backend.matches.dto;

public record BowlingStatDto(
        int balls,
        int runs,
        int wickets,
        int maidens,
        int noBallsBowled,
        int widesBowled
) {}
