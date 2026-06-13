package com.gullycricket.backend.matches.DTOs;

import com.fasterxml.jackson.databind.JsonNode;

public record MatchResponseDto(
        String id,
        String seasonId,
        JsonNode matchData
) {}
