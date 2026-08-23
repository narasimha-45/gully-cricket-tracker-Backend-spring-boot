package com.gullycricket.backend.stats.dto;

public record PlayerParticipationSummaryDto(
        Integer matchesPlayed,
        Integer matchesWon,
        Integer playerOfTheMatchAwards
) {}
