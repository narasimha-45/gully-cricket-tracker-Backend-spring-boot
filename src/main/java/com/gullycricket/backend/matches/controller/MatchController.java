package com.gullycricket.backend.matches.controller;

import com.gullycricket.backend.matches.dto.MatchDataDto;
import com.gullycricket.backend.matches.dto.MatchResponseDto;
import com.gullycricket.backend.matches.service.MatchService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/matches")
@RequiredArgsConstructor
@Tag(name = "Match API", description = "Operations related to matches")
public class MatchController {

    private final MatchService matchService;

    @GetMapping("/{id}")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved match details")
    @ApiResponse(responseCode = "404", description = "Match not found")
    public ResponseEntity<MatchResponseDto> getMatch(@PathVariable String id) {
        return ResponseEntity.ok(matchService.getMatchById(id));
    }

    /**
     * Supports the RESTful POST /matches endpoint and keeps /matches/create for
     * backward compatibility with the existing frontend.
     */
    @PostMapping({"", "/create"})
    @ApiResponse(responseCode = "201", description = "Match created successfully")
    @ApiResponse(responseCode = "200", description = "Existing match returned for an idempotent retry")
    public ResponseEntity<MatchResponseDto> createMatch(
            @Valid @RequestBody MatchDataDto matchData,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        MatchResponseDto response = matchService.saveMatch(matchData, idempotencyKey);
        // Returning 201 for both first create and a retry keeps the existing client contract simple.
        // The body is stable and duplicate inserts are prevented by the idempotency key.
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
