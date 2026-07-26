package com.gullycricket.backend.migration.DTOs;

public record MongoTeamsDTO(
        MongoTeamDTO teamA,
        MongoTeamDTO teamB
) {
}