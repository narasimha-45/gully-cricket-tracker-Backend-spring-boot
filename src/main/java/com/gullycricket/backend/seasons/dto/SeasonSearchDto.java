package com.gullycricket.backend.seasons.dto;

public record SeasonSearchDto(
        String seasonId,
        String seasonName,
        Integer totalMatches
) {
}
