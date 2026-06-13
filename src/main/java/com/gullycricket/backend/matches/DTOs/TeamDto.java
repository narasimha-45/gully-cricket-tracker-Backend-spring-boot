package com.gullycricket.backend.matches.DTOs;

import java.util.List;

public record TeamDto(
        String name,
        List<String> players
) {}