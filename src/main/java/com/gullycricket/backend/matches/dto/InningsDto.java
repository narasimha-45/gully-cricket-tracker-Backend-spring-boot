package com.gullycricket.backend.matches.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

public record InningsDto(
        @NotBlank String battingTeam,
        @NotBlank String bowlingTeam,
        @PositiveOrZero int totalRuns,
        @PositiveOrZero int wickets,
        @PositiveOrZero int balls,
        Map<String, BattingStatDto> battingStats,
        Map<String, BowlingStatDto> bowlingStats,
        ExtrasDto extras,
        Map<String, DismissalDto> dismissals,
        List<BallDto> ballByBall,
        @JsonProperty("isSuperOver") boolean isSuperOver,
        boolean completed,
        Integer inningsNumber,
        String completionReason
) {}
