package com.gullycricket.backend.stats.dto;

import com.gullycricket.backend.matches.entity.MatchType;

public record TeamStatsFilter(
        String seasonId,
        MatchType matchType
) {
}
