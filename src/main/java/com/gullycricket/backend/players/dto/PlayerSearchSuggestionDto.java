package com.gullycricket.backend.players.dto;

import jakarta.persistence.criteria.CriteriaBuilder;

public record PlayerSearchSuggestionDto(
        String playerId,
        String playerName,
        Integer matchesPlayed
) {
}
