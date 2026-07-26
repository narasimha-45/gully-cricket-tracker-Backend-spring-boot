package com.gullycricket.backend.matches.DTOs;

import com.gullycricket.backend.matches.entity.MatchType;

import java.util.List;
import java.util.Map;

public record MatchDataDto(
        String seasonId,
        Map<String, TeamDto> teams,
        TossDto toss,
        RulesDto rules,
        int totalOvers,
        MatchType matchType,
        List<InningsDto> innings,
        ResultDto result
) {}
