package com.gullycricket.backend.migration.controller;

import com.gullycricket.backend.migration.dto.MigrationSummaryDto;
import com.gullycricket.backend.migration.service.MongoToPostgresMigrationService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * One-off admin endpoint for the Mongo -> Postgres migration. Not meant to stay
 * around long-term — once the migration has been run against production data and
 * verified, this controller, the migration service, and the Mongo entity/repository
 * classes under this package can all be deleted, along with the Mongo datasource
 * config and the spring-boot-starter-data-mongodb dependency in pom.xml.
 */
@Slf4j
@Profile("migration")
@RestController
@RequestMapping("/migration")
@RequiredArgsConstructor
@Tag(name = "Migration API", description = "One-off Mongo to Postgres data migration")
public class MigrationController {

    private final MongoToPostgresMigrationService migrationService;

    @PostMapping("/run")
    @Tag(name = "Run Migration", description = "Migrate all seasons and matches from Mongo into Postgres")
    @ApiResponse(responseCode = "200", description = "Migration finished (see body for per-match failures)")
    public ResponseEntity<MigrationSummaryDto> runMigration() {
        log.info("Mongo -> Postgres migration triggered");
        MigrationSummaryDto summary = migrationService.migrateData();
        return ResponseEntity.ok(summary);
    }
}
