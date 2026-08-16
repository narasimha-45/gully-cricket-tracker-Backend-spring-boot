package com.gullycricket.backend.migration.dto;

import java.util.List;

public record MongoTeamDTO(

        String name,
        List<String> players

) {
}
