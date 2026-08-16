package com.gullycricket.backend.matches.dto;

public record BattingStatDto(
        int battingPosition,
        int runs,
        int balls,
        int fours,
        int sixes,
        DismissalDto dismissal   // nullable
) {}
