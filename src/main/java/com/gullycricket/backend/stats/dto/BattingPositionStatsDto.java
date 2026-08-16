package com.gullycricket.backend.stats.dto;

public record BattingPositionStatsDto(
        Integer battingPosition,
        Integer innings,
        Integer notOuts,
        Integer totalRuns,
        Integer totalBallsFaced,
        Double average,
        Double strikeRate,
        Integer highestScore,
        Integer fours,
        Integer sixes
) {
}
