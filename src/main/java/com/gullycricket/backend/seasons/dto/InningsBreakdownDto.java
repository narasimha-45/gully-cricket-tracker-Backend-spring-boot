package com.gullycricket.backend.seasons.dto;

public record InningsBreakdownDto(
        int inningsNumber,
        int runs,
        int wickets,
        int balls,
        boolean completed,
        boolean followOn,
        String completionReason
) {}
