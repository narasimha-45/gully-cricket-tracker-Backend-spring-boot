package com.gullycricket.backend.stat.controller;


import com.gullycricket.backend.matches.respository.MatchRepository;
import com.gullycricket.backend.players.repository.PlayerMatchRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stats")
@AllArgsConstructor
public class StatsController {

    private final PlayerMatchRepository playerMatchRepository;
    private final MatchRepository matchRepository;

    @GetMapping("/player/{playerId}")
    public ResponseEntity<?> getPlayerStats(@PathVariable String playerId) {
        // Implement logic to fetch player stats using playerMatchRepository
        return ResponseEntity.ok().build();
    }

    @GetMapping("/team/{teamId}")
    public ResponseEntity<?> getTeamStats(@PathVariable String teamId) {
        // Implement logic to fetch team stats using matchRepository
        return ResponseEntity.ok().build();
    }

    @GetMapping("/battingStats")
    public ResponseEntity<?> getBattingStats(
            @RequestParam String seasonId,
            @RequestParam String matchType,
            @RequestParam String playedTeamId,
            @RequestParam String oppositionTeamId,
            @RequestParam String battingPosition,
            @RequestParam String inningsNumber,
            @RequestParam String matchResult
    ) {
        // Implement logic to fetch batting stats
        return ResponseEntity.ok().build();
    }

}
