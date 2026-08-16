package com.gullycricket.backend.stats.DTOs;

import java.time.LocalDateTime;

public record RecentInningDto(
        String matchId,
        String seasonId,
        String seasonName,
        String teamId,
        String teamName,
        String opponentTeamId,
        String opponentTeamName,
        Integer battingPosition,
        Integer runsScored,
        Integer ballsFaced,
        Integer foursHit,
        Integer sixesHit,
        boolean out,
        boolean matchWon,
        LocalDateTime completedAt
) {
}
