package com.gullycricket.backend.matches.DTOs;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BallDto(
        int over,
        int ballInOver,
        int actualBallNum,
        String striker,
        String nonStriker,
        String bowler,
        int runs,
        String type,
        @JsonProperty("isWicket") boolean isWicket,
        WicketDto wicket,         // null when isWicket = false
        long timestamp
) {}
