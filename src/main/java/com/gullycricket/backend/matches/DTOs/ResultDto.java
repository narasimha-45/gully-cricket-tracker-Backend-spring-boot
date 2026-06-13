package com.gullycricket.backend.matches.DTOs;

public record ResultDto(
        String winner,
        String type,
        int margin,
        String manOfTheMatch
) {}
