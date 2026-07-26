package com.gullycricket.backend.stat.DTOs;

import com.gullycricket.backend.matches.entity.MatchType;
import com.gullycricket.backend.stat.enums.MatchResult;

public record BowlingStatsFilter(
        String seasonId,
        MatchType matchType,
        String teamId,
        String opponentTeamId,
        Integer inningsNumber,
        MatchResult result
) {}