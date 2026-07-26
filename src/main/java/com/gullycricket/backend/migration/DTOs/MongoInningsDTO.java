package com.gullycricket.backend.migration.DTOs;

import java.util.Map;

public record MongoInningsDTO(

        String battingTeam,
        String bowlingTeam,

        Integer totalRuns,
        Integer wickets,
        Integer balls,

        Map<String, MongoBattingStatsDTO> battingStats,
        Map<String, MongoBowlingStatsDTO> bowlingStats,

        MongoExtrasDTO extras,

        Map<String, MongoDismissalDTO> dismissals,

        Boolean completed

) {
}