package com.gullycricket.backend.seasons.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSeasonRequest(
        @NotBlank @Size(max = 100) String seasonName
) {}
