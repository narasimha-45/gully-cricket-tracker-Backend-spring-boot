package com.gullycricket.backend.matches.DTOs;

import java.util.List;
import java.util.Map;

public record MatchDataDto(
        String seasonId,
        Map<String, TeamDto> teams,
        TossDto toss,
        RulesDto rules,
        int totalOvers,
        String matchType,
        List<InningsDto> innings,
        ResultDto result
) {}
