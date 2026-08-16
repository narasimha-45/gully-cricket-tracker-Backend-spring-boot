package com.gullycricket.backend.matches.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record TeamDto(
        @NotBlank String name,
        @NotEmpty List<@NotBlank String> players
) {}
