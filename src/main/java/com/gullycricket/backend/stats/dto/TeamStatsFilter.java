package com.gullycricket.backend.stats.dto;

import com.gullycricket.backend.matches.entity.MatchType;

import java.util.List;

public record TeamStatsFilter(
        List<String> seasonIds,
        MatchType matchType
) {
}
