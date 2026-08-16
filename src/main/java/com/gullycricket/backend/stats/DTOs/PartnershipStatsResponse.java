package com.gullycricket.backend.stats.DTOs;

public record PartnershipStatsResponse(
        String player1Id,
        String player1Name,
        String player2Id,
        String player2Name,

        Integer totalInnings,

        Integer totalRuns,
        Integer totalBallsFaced,
        Integer totalDotBalls,
        Integer totalFours,
        Integer totalSixes,

        Double runRate,
        Double averagePartnership,
        Double dotBallPercentage,

        Integer highestPartnership,
        Integer lowestPartnership,
        Integer unbeatenPartnerships,

        Integer player1Runs,
        Integer player2Runs,

        Integer player1BallsFaced,
        Integer player2BallsFaced,

        Integer player1Fours,
        Integer player2Fours,

        Integer player1Sixes,
        Integer player2Sixes,

        Integer player1DotBalls,
        Integer player2DotBalls
) {
}
