package com.gullycricket.backend.seasons.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.gullycricket.backend.matches.entity.MatchStatus;
import com.gullycricket.backend.matches.entity.MatchType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * teamAScore/teamAWickets/teamABallsFaced (and the teamB equivalents) are the all-innings
 * total for that team — correct as-is for a single-innings (OVERS) match, but a combined
 * total across both innings for a Test (matchType == TEST). Clients rendering a match card
 * should check matchType and, for TEST, use teamAInnings/teamBInnings to show each innings
 * separately (e.g. "286 & 177-7") instead of the merged total.
 */
public record MatchResponseDto(
        String id,
        String seasonId,
        String teamA,
        Integer teamAScore,
        Integer teamAWickets,
        List<InningsScoreDto> teamAInnings,
        String teamB,
        Integer teamBScore,
        Integer teamBWickets,
        List<InningsScoreDto> teamBInnings,
        String winner,
        Boolean superOver,
        String wonBy,
        LocalDateTime completedAt,
        MatchStatus matchStatus,
        MatchType matchType,
        Integer teamABallsFaced,
        Integer teamBBallsFaced,
        Integer totalOvers
) {}
