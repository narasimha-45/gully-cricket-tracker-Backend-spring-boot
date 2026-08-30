package com.gullycricket.backend.seasons.dto;

/** Innings-level score used to render Test matches without merging both innings. */
public record MatchInningsSummaryDto(
        Integer sequenceNumber,
        Integer teamInningsNumber,
        String battingTeam,
        Integer runs,
        Integer wickets,
        Integer balls,
        Boolean superOver,
        Boolean completed
) {
}
