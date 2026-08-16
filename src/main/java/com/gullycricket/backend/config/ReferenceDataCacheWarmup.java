package com.gullycricket.backend.config;

import com.gullycricket.backend.seasons.service.SeasonService;
import com.gullycricket.backend.teams.service.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Warms the database pool and tiny reference-data caches before ApplicationReady.
 * This keeps the first UI request from paying both a cold connection and a reference query.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReferenceDataCacheWarmup implements ApplicationRunner {

    private final SeasonService seasonService;
    private final TeamService teamService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            seasonService.getAllSeasons();
            teamService.getAllTeams();
            log.debug("Reference-data caches and database pool warmed");
        } catch (RuntimeException ex) {
            // Optional optimization: startup should not fail solely because pre-warming
            // failed. Readiness/health and the first real request will surface a DB issue.
            log.warn("Reference-data cache warmup failed: {}", ex.getMessage());
        }
    }
}
