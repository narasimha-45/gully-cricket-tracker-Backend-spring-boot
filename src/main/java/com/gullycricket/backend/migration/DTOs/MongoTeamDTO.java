package com.gullycricket.backend.migration.DTOs;

import java.util.List;

public record MongoTeamDTO(

        String name,
        List<String> players

) {
}