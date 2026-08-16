package com.gullycricket.backend.migration.dto;

public record MongoBowlingStatsDTO(

        Integer balls,
        Integer runs,
        Integer wickets,
        Integer maidens

) {
}
