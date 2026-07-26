package com.gullycricket.backend.seasons.controller;

import com.gullycricket.backend.seasons.DTOs.CreateSeasonRequest;
import com.gullycricket.backend.seasons.DTOs.MatchResponseDto;
import com.gullycricket.backend.seasons.DTOs.SeasonSearchDto;
import com.gullycricket.backend.seasons.entity.Season;
import com.gullycricket.backend.seasons.service.SeasonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/seasons")
@Tag(name = "Season API", description = "Operations related to seasons")
public class SeasonController {

    @Autowired
    private SeasonService seasonService;

    @GetMapping
    @Operation(
            summary = "Get all seasons",
            description = "Returns all available cricket seasons"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved all seasons"
    )
    public ResponseEntity<List<Season>> getAllSeasons(){
        log.info("Getting all Seasons");
        return ResponseEntity.ok(seasonService.getAllSeasons());
    }

    @GetMapping("/{seasonId}")
    @Operation(
            summary = "Get season details",
            description = "Returns details of a specific season by its ID"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved season details"
    )
    public ResponseEntity<Season> getSeason(@PathVariable String seasonId){
        log.info("getting season details with {}",seasonId);
        return ResponseEntity.ok(seasonService.getSeasonById(seasonId));
    }

    @PostMapping("/create")
    @Operation(
            summary = "Create a new season",
            description = "Creates a new cricket season"
    )
    @ApiResponse(
            responseCode = "201",
            description = "Season created successfully"
    )
    public ResponseEntity<Season> createSeason(@RequestBody CreateSeasonRequest request){
        log.info("Creating new season with name {}", request.seasonName());
        Season season = seasonService.createSeason(request.seasonName());
        return ResponseEntity.status(201).body(season);
    }

    @GetMapping("/matches/{seasonId}")
    @Operation(
            summary = "Get all matches for a season",
            description = "Returns all matches for a specific season"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved all matches for the season"
    )
    public ResponseEntity<List<MatchResponseDto>> getMatchesForSeason(@PathVariable String seasonId) {
        log.info("Getting all matches for season with ID {}", seasonId);
        List<MatchResponseDto> matches = seasonService.getALlMatchesBySeasonId(seasonId);
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/search")
    @Operation(
            summary = "Get all seasons matching the query",
            description = "Return all seasons matching the query"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved all seasons for the query"
    )
    public ResponseEntity<List<SeasonSearchDto>> searchSeason(@RequestParam String query){
        log.info("Requested to search for seasons with query:{}",query);
        return ResponseEntity.ok(seasonService.searchSeasons(query));
    }

}
