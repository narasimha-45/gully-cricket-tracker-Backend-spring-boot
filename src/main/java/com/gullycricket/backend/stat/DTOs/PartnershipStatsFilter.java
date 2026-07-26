package com.gullycricket.backend.stat.DTOs;

import com.gullycricket.backend.matches.entity.MatchType;
import com.gullycricket.backend.stat.enums.MatchResult;

public record PartnershipStatsFilter(
        String seasonId,
        MatchType matchType,
        String teamId,
        String opponentTeamId,
        Integer inningsNumber,
        MatchResult result,
        Integer partnershipNumber,
        String playerId,
        String partnerId,
        Boolean battingFirst
) {
}
