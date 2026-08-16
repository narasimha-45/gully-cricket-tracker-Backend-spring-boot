package com.gullycricket.backend.matches.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record MatchResponseDto(
        String id,
        String seasonId,
        JsonNode matchData
) {}
