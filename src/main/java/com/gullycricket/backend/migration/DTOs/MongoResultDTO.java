package com.gullycricket.backend.migration.DTOs;

public record MongoResultDTO(

        String winner,
        String type,
        Integer margin,
        String manOfTheMatch

) {
}