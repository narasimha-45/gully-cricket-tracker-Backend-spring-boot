package com.gullycricket.backend.migration.DTOs;

public record MongoBattingStatsDTO(

        Integer runs,
        Integer balls,
        Integer fours,
        Integer sixes,

        MongoDismissalDTO dismissal

) {
}