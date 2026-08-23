package com.gullycricket.backend.stats.dto;

public record RecentBattingPerformanceDto(
        Integer inningsNumber,
        Integer battingPosition,
        Integer runsScored,
        Integer ballsFaced,
        Integer foursHit,
        Integer sixesHit,
        boolean out
) {}
