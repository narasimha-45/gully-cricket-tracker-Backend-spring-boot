package com.gullycricket.backend.stats.DTOs;

import com.gullycricket.backend.matches.entity.MatchType;
import com.gullycricket.backend.stats.enums.MatchResult;

public record BattingStatsFilter(
        String seasonId,
        MatchType matchType,
        String teamId,
        String opponentTeamId,
        Integer battingPosition,
        Integer inningsNumber,
        MatchResult result
) {
}
