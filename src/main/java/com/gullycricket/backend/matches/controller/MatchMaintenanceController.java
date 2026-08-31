package com.gullycricket.backend.matches.controller;

import com.gullycricket.backend.matches.dto.RebuildResultDto;
import com.gullycricket.backend.matches.service.MatchRebuildService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/rebuild")
@RequiredArgsConstructor
@Tag(name = "Match Rebuild API", description = "Administrative projection rebuild operations")
public class MatchMaintenanceController {

    private final MatchRebuildService matchRebuildService;

    @PostMapping("/matches/{matchId}")
    public ResponseEntity<RebuildResultDto> rebuildMatch(@PathVariable String matchId) {
        return ResponseEntity.ok(matchRebuildService.rebuildMatch(matchId));
    }

    @PostMapping("/seasons/{seasonId}")
    public ResponseEntity<RebuildResultDto> rebuildSeason(@PathVariable String seasonId) {
        return ResponseEntity.ok(matchRebuildService.rebuildSeason(seasonId));
    }

    @PostMapping("/all")
    public ResponseEntity<RebuildResultDto> rebuildAll() {
        return ResponseEntity.ok(matchRebuildService.rebuildAll());
    }
}
