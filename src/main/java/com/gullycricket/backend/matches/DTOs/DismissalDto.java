package com.gullycricket.backend.matches.DTOs;

public record DismissalDto(
        String type,
        String bowler,
        String fielder            // nullable
) {}
