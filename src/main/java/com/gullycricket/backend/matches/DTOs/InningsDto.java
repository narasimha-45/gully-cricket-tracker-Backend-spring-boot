package com.gullycricket.backend.matches.DTOs;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record InningsDto(
        String battingTeam,
        String bowlingTeam,
        int totalRuns,
        int wickets,
        int balls,
        Map<String, BattingStatDto> battingStats,
        Map<String, BowlingStatDto> bowlingStats,
        ExtrasDto extras,
        Map<String, DismissalDto> dismissals,
        List<BallDto> ballByBall,
        @JsonProperty("isSuperOver") boolean isSuperOver,
        boolean completed
) {}
