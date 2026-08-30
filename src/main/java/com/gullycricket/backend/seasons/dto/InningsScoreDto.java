package com.gullycricket.backend.seasons.dto;

public record InningsScoreDto(
        Integer inningsNumber,
        int runs,
        int wickets,
        int balls,
        boolean completed
) {}
