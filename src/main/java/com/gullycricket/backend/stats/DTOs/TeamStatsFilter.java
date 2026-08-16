package com.gullycricket.backend.stats.DTOs;

import com.gullycricket.backend.matches.entity.MatchType;

public record TeamStatsFilter(
        String seasonId,
        MatchType matchType
) {
}
