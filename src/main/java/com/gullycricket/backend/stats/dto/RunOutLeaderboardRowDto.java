package com.gullycricket.backend.stats.dto;

/** One batting partnership pair, grouped across the selected seasons. */
public record RunOutLeaderboardRowDto(
        String player1Id, String player1Name, String player2Id, String player2Name,
        int partnerships, int runOuts, int player1Out, int player2Out,
        int unknownPartnerships, Double runOutRate) {}
