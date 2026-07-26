package com.gullycricket.backend.stat.DTOs;

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
