package com.gullycricket.backend.seasons.dto;

import java.time.LocalDateTime;

public record SeasonDto(
        String id,
        String seasonName,
        Integer matchesPlayed,
        LocalDateTime createdAt
) {}
