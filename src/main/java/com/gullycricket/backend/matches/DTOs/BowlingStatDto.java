package com.gullycricket.backend.matches.DTOs;

public record BowlingStatDto(
        int balls,
        int runs,
        int wickets,
        int maidens
) {}
