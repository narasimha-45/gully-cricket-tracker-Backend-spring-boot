package com.gullycricket.backend.matches.dto;

import com.gullycricket.backend.players.entity.DismissalType;

public record DismissalDto(
        DismissalType type,
        String bowler,
        String fielder            // nullable
) {}
