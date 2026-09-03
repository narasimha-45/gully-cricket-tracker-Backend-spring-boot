package com.gullycricket.backend.matches.live.dto;

public record LiveInningsScoreDto(
        String battingTeam,
        int inningsNumber,
        int runs,
        int wickets,
        int balls,
        String completionReason,
        boolean followOn,
        boolean superOver,
        boolean completed
) {
}
