package com.gullycricket.backend.migration.dto;

import java.util.List;

public record MigrationSummaryDto(
        int seasonsMigrated,
        int totalMatches,
        int matchesMigrated,
        List<String> failures
) {
}
