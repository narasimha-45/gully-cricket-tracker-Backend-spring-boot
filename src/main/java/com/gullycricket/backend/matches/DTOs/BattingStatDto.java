package com.gullycricket.backend.matches.DTOs;

public record BattingStatDto(
        int battingPosition,
        int runs,
        int balls,
        int fours,
        int sixes,
        DismissalDto dismissal   // nullable
) {}
