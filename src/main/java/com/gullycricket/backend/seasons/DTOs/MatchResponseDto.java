package com.gullycricket.backend.seasons.DTOs;

import com.fasterxml.jackson.databind.JsonNode;
import com.gullycricket.backend.matches.entity.MatchStatus;

import java.time.LocalDateTime;

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
        Integer teamABallsFaced,
        Integer teamBBallsFaced,
        Integer totalOvers
) {}
