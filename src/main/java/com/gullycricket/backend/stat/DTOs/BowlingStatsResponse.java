package com.gullycricket.backend.stat.DTOs;

import com.gullycricket.backend.stat.enums.BestBowlingFigures;

public record BowlingStatsResponse(
        String playerId,
        String playerName,
        Integer totalWickets,
        Integer totalRunsConceded,
        Double economyRate,
        Integer totalOversBowled,
        Integer totalMaidens,
        Double average,
        BestBowlingFigures bestBowlingFigures,
        Integer fiveWicketHauls,
        Integer tenWicketHauls,
        Integer totalMatchesPlayed,
        Integer inningsBowled,
        Integer dotBallsBowled
) {
}
