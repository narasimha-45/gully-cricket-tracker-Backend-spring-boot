package com.gullycricket.backend.seasons.DTOs;

public record SeasonSearchDto(
        String seasonId,
        String seasonName,
        Integer totalMatches
) {
}
