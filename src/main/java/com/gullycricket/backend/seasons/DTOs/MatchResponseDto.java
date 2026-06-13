package com.gullycricket.backend.seasons.DTOs;

import com.fasterxml.jackson.databind.JsonNode;
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
        LocalDateTime completedAt
) {}
