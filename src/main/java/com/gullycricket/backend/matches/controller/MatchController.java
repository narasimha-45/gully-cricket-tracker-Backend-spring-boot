package com.gullycricket.backend.matches.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.gullycricket.backend.matches.DTOs.MatchResponseDto;
import com.gullycricket.backend.matches.service.MatchService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/matches")
@Tag(name = "Match API", description = "Operations related to matches")
public class MatchController {

    @Autowired
    private MatchService matchService;

    @GetMapping("/{id}")
    @Tag(name = "Get Match", description = "Retrieve match details by ID")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved match details")
    @ApiResponse(responseCode = "404", description = "Match not found")
    public ResponseEntity<MatchResponseDto> getMatch(@PathVariable String id) {
        log.info("Getting match details with ID {}", id);
        MatchResponseDto match = matchService.getMatchById(id);
        return ResponseEntity.ok(match);
    }

    @PostMapping("/create")
    @Tag(name = "Create Match", description = "Create a new match")
    @ApiResponse(responseCode = "201", description = "Match created successfully")
    public ResponseEntity<MatchResponseDto> createMatch(@RequestBody JsonNode matchData) {
        log.info("Creating new match with data: {}", matchData);
        MatchResponseDto response = matchService.saveMatch(matchData);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
