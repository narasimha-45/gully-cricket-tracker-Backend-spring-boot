package com.gullycricket.backend.matches.DTOs;

public record WicketDto(
        String type,
        String outBatsman,
        String helper             // nullable
) {}
