package com.gullycricket.backend.stats.dto;

public record PlayerComparisonDto(
        String seasonId,
        PlayerComparisonSideDto player1,
        PlayerComparisonSideDto player2
) {}
