package com.gullycricket.backend.stats.dto;

import com.gullycricket.backend.matches.entity.MatchType;
import com.gullycricket.backend.stats.enums.MatchResult;

import java.util.List;

public record BowlingStatsFilter(
        List<String> seasonIds,
        MatchType matchType,
        List<String> teamIds,
        List<String> opponentTeamIds,
        List<Integer> inningsNumbers,
        List<MatchResult> results
) {}
