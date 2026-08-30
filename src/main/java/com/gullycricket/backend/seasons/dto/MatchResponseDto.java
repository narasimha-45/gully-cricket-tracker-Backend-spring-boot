package com.gullycricket.backend.seasons.dto;

import com.gullycricket.backend.matches.entity.MatchStatus;
import com.gullycricket.backend.matches.entity.MatchType;

import java.time.LocalDateTime;
import java.util.List;

public record MatchResponseDto(
        String id,
        String seasonId,
        String teamA,
        Integer teamAScore,
        Integer teamAWickets,
        String teamB,
        Integer teamBScore,
        Integer teamBWickets,
        String winner,
        Boolean superOver,
        String wonBy,
        LocalDateTime completedAt,
        MatchStatus matchStatus,
        MatchType matchType,
        Integer teamABallsFaced,
        Integer teamBBallsFaced,
        Integer totalOvers,
        List<MatchInningsSummaryDto> innings
) {}
