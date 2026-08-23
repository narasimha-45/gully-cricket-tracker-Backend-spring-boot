package com.gullycricket.backend.stats.dto;

import com.gullycricket.backend.stats.enums.MatchResult;
import java.time.LocalDateTime;
import java.util.List;

public record RecentPerformanceDto(
        String matchId,
        String seasonId,
        String seasonName,
        String teamId,
        String teamName,
        String opponentTeamId,
        String opponentTeamName,
        boolean matchWon,
        MatchResult result,
        LocalDateTime completedAt,
        List<RecentBattingPerformanceDto> batting,
        List<RecentBowlingPerformanceDto> bowling,
        Integer catches,
        Integer runOuts,
        Integer stumpings
) {}
