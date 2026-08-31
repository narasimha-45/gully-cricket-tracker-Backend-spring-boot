package com.gullycricket.backend.stats.dto;

import com.gullycricket.backend.matches.entity.MatchType;
import com.gullycricket.backend.stats.enums.MatchResult;

public record PlayerComparisonFilter(
        String seasonId,
        MatchType matchType,
        String teamId,
        String opponentTeamId,
        MatchResult result,

        Integer battingInningsNumber,
        Integer battingPosition,
        Integer minBallsFaced,

        Integer bowlingInningsNumber,
        Integer minOversBowled
) {
}