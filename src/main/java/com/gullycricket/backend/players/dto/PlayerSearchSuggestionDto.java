package com.gullycricket.backend.players.dto;

public record PlayerSearchSuggestionDto(
        String playerId,
        String playerName,
        Integer matchesPlayed
) {
}
