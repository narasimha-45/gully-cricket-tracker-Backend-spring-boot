package com.gullycricket.backend.stats.dto;

public record RecentBowlingPerformanceDto(
        Integer inningsNumber,
        Integer wicketsTaken,
        Integer runsConceded,
        Integer ballsBowled,
        Double oversBowled,
        Integer maidensBowled,
        Integer dotBallsBowled
) {}
