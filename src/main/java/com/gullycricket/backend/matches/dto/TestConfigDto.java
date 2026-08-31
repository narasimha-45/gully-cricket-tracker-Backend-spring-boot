package com.gullycricket.backend.matches.dto;

/**
 * Mirrors the frontend's match.testConfig shape ({@code inningsPerTeam}, {@code followOnEnforced}).
 * Only meaningful when matchType is TEST; null/omitted for limited-overs matches.
 */
public record TestConfigDto(
        Integer inningsPerTeam,
        Boolean followOnEnforced
) {}
