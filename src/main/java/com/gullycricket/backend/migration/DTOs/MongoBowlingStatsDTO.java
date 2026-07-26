package com.gullycricket.backend.migration.DTOs;

public record MongoBowlingStatsDTO(

        Integer balls,
        Integer runs,
        Integer wickets,
        Integer maidens

) {
}