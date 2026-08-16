package com.gullycricket.backend.migration.dto;

public record MongoResultDTO(

        String winner,
        String type,
        Integer margin,
        String manOfTheMatch

) {
}
