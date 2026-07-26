package com.gullycricket.backend.stat.DTOs;

import com.gullycricket.backend.matches.entity.MatchType;
import com.gullycricket.backend.stat.enums.MatchResult;

public record RivalryStatsFilter(
        String seasonId,
        MatchType matchType,
        String teamId,
        String opponentTeamId,
        Integer inningsNumber,
        MatchResult matchResult,
        String batsmanId,
        String bowlerId,
        Integer minBallsFaced,
        Integer minRuns,
        Integer minDismissals
) {
}
