package com.gullycricket.backend.stats.DTOs;

public record BattingStatsResponse(
        String playerId,
        String playerName,
        Integer totalRuns,
        Integer totalBallsFaced,
        Double strikeRate,
        Integer totalFours,
        Integer totalSixes,
        Integer notOuts,
        Double average,
        Integer highestScore,
        Integer ducks,
        Integer totalMatchesPlayed,
        Integer inningsPlayed,
        Integer dotBalls
) {
}
