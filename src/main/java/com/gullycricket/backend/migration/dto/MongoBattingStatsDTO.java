package com.gullycricket.backend.migration.dto;

public record MongoBattingStatsDTO(

        Integer runs,
        Integer balls,
        Integer fours,
        Integer sixes,

        MongoDismissalDTO dismissal

) {
}
