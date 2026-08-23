package com.gullycricket.backend.stats.dto;

import com.gullycricket.backend.players.entity.DismissalType;

import java.time.LocalDateTime;

public record RivalryInningsDto(
        String rivalryId,
        String matchId,
        String seasonId,
        String seasonName,
        String batterTeamId,
        String batterTeamName,
        String opponentTeamId,
        String opponentTeamName,
        String batterId,
        String batterName,
        String bowlerId,
        String bowlerName,
        Integer inningsNumber,
        Integer runsScored,
        Integer ballsFaced,
        Integer dotBalls,
        Integer fours,
        Integer sixes,
        Double strikeRate,
        boolean batterDismissed,
        DismissalType dismissalType,
        boolean matchWon,
        LocalDateTime completedAt
) {}
