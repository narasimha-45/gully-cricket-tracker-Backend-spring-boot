package com.gullycricket.backend.stats.dto;

import java.util.List;

public record BattingRunOutStatsDto(
        String playerId,
        int involved,
        int personallyOut,
        int partnerOut,
        int unknownPartnerships,
        List<Partner> partners
) {
    public record Partner(String playerId, String playerName, int partnerships, int runOuts,
                          int playerOut, int partnerOut, int unknownPartnerships, Double runOutRate) {}
}
