package com.gullycricket.backend.migration.dto;

import java.time.Instant;
import java.util.List;

public record MongoMatchDTO(

        String id,
        String seasonId,
        String matchType,

        MongoTeamsDTO teams,
        MongoTossDTO toss,
        MongoRulesDTO rules,

        Integer totalOvers,

        List<MongoInningsDTO> innings,

        MongoResultDTO result,

        String status,

        Instant completedAt,
        Instant createdAt,
        Instant updatedAt

) {
}
