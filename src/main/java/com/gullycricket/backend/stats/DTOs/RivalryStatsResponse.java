package com.gullycricket.backend.stats.DTOs;

public record RivalryStatsResponse(
        String batterId,
        String batterName,
        String bowlerId,
        String bowlerName,
        Integer totalInnings,
        Integer totalRuns,
        Integer totalBallsFaced,
        Integer totalDotBalls,
        Integer totalFours,
        Integer totalSixes,
        Double strikeRate,
        Double averageRuns,
        Double dotBallPercentage,
        Integer wicketsTaken
) {
}
