package com.gullycricket.backend.migration.DTOs;

import java.util.List;

public record MigrationSummaryDto(
        int seasonsMigrated,
        int totalMatches,
        int matchesMigrated,
        List<String> failures
) {
}