package com.gullycricket.backend.stats.dto;

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
