package com.gullycricket.backend.matches.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Test-match-only configuration persisted with match_data for accurate replay. */
public record TestConfigDto(
        @Min(1) @Max(2) Integer inningsPerTeam,
        Boolean followOnEnforced
) {
}
