package com.gullycricket.backend.stats.dto;

public record PlayerVsPlayerDto(
        String seasonId,
        String player1Id,
        String player1Name,
        String player2Id,
        String player2Name,
        RivalryStatsResponse player1BattingVsPlayer2,
        RivalryStatsResponse player2BattingVsPlayer1,
        PartnershipStatsResponse partnershipTogether
) {}
