package com.gullycricket.backend.matches.dto;

import com.gullycricket.backend.matches.entity.MatchType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;
import java.util.Map;

public record MatchDataDto(
        @NotBlank String seasonId,
        @NotNull Map<String, @Valid TeamDto> teams,
        @Valid TossDto toss,
        @Valid RulesDto rules,
        @PositiveOrZero int totalOvers,
        @NotNull MatchType matchType,
        @Valid TestConfigDto testConfig,
        @NotEmpty List<@Valid InningsDto> innings,
        @Valid ResultDto result
) {}
