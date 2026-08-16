package com.gullycricket.backend.stats.dto;

import com.gullycricket.backend.stats.enums.BestBowlingFigures;

public record BowlingStatsResponse(
        String playerId,
        String playerName,
        Integer totalWickets,
        Integer totalRunsConceded,
        Double economyRate,
        Double totalOversBowled,
        Integer totalMaidens,
        Double average,
        BestBowlingFigures bestBowlingFigures,
        Integer fiveWicketHauls,
        Integer tenWicketHauls,
        Integer totalMatchesPlayed,
        Integer inningsBowled,
        Integer dotBallsBowled
) {}
