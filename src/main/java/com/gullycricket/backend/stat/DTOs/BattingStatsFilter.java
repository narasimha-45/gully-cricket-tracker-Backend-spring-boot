package com.gullycricket.backend.stat.DTOs;

import com.gullycricket.backend.matches.entity.MatchType;
import com.gullycricket.backend.stat.enums.MatchResult;

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
