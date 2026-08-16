package com.gullycricket.backend.migration.dto;

public record MongoTeamsDTO(
        MongoTeamDTO teamA,
        MongoTeamDTO teamB
) {
}
