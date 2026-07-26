package com.gullycricket.backend.players.controller;

import com.gullycricket.backend.players.DTOs.PlayerSearchSuggestionDto;
import com.gullycricket.backend.players.service.PlayerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/players")
@Tag(name = "Player Search API", description = "Operations related to searching players")
public class PlayerSearchController {

    @Autowired
    private PlayerService playerService;

    @Operation(description = "Search for players based on a query string")
    @GetMapping("/search")
    public ResponseEntity<List<PlayerSearchSuggestionDto>> searchPlayers(@RequestParam String query) {
        log.info("Received search request for players with query: {}", query);
        List<PlayerSearchSuggestionDto> response = playerService.searchPlayers(query);
        return ResponseEntity.ok(response);
    }

//    @GetMapping("/profile")
//    public ResponseEntity<PlayerProfileDto> getPlayerProfile(@RequestParam String id) {
//        log.info("Received request for player profile with ID: {}", id);
//        PlayerProfileDto response = playerService.getPlayerProfile(id);
//        return ResponseEntity.ok(response);
//    }
}
