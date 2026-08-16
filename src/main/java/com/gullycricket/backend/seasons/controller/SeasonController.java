package com.gullycricket.backend.seasons.controller;

import com.gullycricket.backend.seasons.dto.CreateSeasonRequest;
import com.gullycricket.backend.seasons.dto.MatchResponseDto;
import com.gullycricket.backend.seasons.dto.SeasonDto;
import com.gullycricket.backend.seasons.dto.SeasonSearchDto;
import com.gullycricket.backend.seasons.entity.Season;
import com.gullycricket.backend.seasons.service.SeasonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/seasons")
@RequiredArgsConstructor
@Tag(name = "Season API", description = "Operations related to seasons")
public class SeasonController {

    private final SeasonService seasonService;

    @GetMapping
    @Operation(summary = "Get all seasons")
    public ResponseEntity<List<SeasonDto>> getAllSeasons() {
        return ResponseEntity.ok(seasonService.getAllSeasons().stream().map(this::toDto).toList());
    }

    @GetMapping("/{seasonId}")
    @Operation(summary = "Get season details")
    public ResponseEntity<SeasonDto> getSeason(@PathVariable String seasonId) {
        return ResponseEntity.ok(toDto(seasonService.getSeasonById(seasonId)));
    }

    @PostMapping({"", "/create"})
    @Operation(summary = "Create a new season")
    public ResponseEntity<SeasonDto> createSeason(@Valid @RequestBody CreateSeasonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toDto(seasonService.createSeason(request.seasonName())));
    }

    @GetMapping("/matches/{seasonId}")
    @Operation(summary = "Get all matches for a season")
    public ResponseEntity<List<MatchResponseDto>> getMatchesForSeason(@PathVariable String seasonId) {
        return ResponseEntity.ok(seasonService.getAllMatchesBySeasonId(seasonId));
    }

    @GetMapping("/search")
    @Operation(summary = "Search seasons")
    public ResponseEntity<List<SeasonSearchDto>> searchSeason(@RequestParam String query) {
        return ResponseEntity.ok(seasonService.searchSeasons(query));
    }

    private SeasonDto toDto(Season season) {
        return new SeasonDto(season.getId(), season.getSeasonName(), season.getMatchesPlayed(), season.getCreatedAt());
    }
}
