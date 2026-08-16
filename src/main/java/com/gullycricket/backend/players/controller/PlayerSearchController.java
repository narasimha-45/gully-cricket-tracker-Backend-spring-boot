package com.gullycricket.backend.players.controller;

import com.gullycricket.backend.players.dto.PlayerSearchSuggestionDto;
import com.gullycricket.backend.players.service.PlayerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/players")
@RequiredArgsConstructor
@Tag(name = "Player Search API", description = "Operations related to searching players")
public class PlayerSearchController {

    private final PlayerService playerService;

    @Operation(description = "Search for players based on a query string")
    @GetMapping("/search")
    public ResponseEntity<List<PlayerSearchSuggestionDto>> searchPlayers(@RequestParam String query) {
        return ResponseEntity.ok(playerService.searchPlayers(query));
    }
}
