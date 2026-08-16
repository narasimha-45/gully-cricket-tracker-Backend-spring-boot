package com.gullycricket.backend.matches.dto;

public record ResultDto(
        String winner,
        String type,
        int margin,
        String manOfTheMatch
) {}
