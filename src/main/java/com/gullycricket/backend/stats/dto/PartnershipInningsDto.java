package com.gullycricket.backend.stats.dto;

import java.time.LocalDateTime;


public record PartnershipInningsDto(
        String id,
        String matchId,
        String seasonId,
        String seasonName,
        String teamRepresentedId,
        String teamName,
        String opponentTeamId,
        String opponentTeamName,
        String player1Id,
        String player1Name,
        String player2Id,
        String player2Name,
        Integer inningsNumber,
        Integer partnershipNumber,
        Integer runsScored,
        Integer ballsFaced,
        Integer player1Runs,
        Integer player1BallsFaced,
        Integer player2Runs,
        Integer player2BallsFaced,
        Boolean partnershipBroken,
        String whoGotOutId,
        Boolean matchWon,
        LocalDateTime completedAt,
        Integer foursHit,
        Integer sixesHit,
        Integer player1FoursHit,
        Integer player1SixesHit,
        Integer player2FoursHit,
        Integer player2SixesHit
) {
}