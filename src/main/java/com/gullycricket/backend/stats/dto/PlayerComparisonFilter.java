package com.gullycricket.backend.stats.dto;

import com.gullycricket.backend.matches.entity.MatchType;
import com.gullycricket.backend.stats.enums.MatchResult;

import java.util.List;

public record PlayerComparisonFilter(
        List<String> seasonIds,
        MatchType matchType,
        List<String> teamIds,
        List<String> opponentTeamIds,
        List<MatchResult> results,

        List<Integer> battingInningsNumbers,
        List<Integer> battingPositions,
        Integer minBallsFaced,

        List<Integer> bowlingInningsNumbers,
        Integer minOversBowled
) {
}
