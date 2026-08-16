package com.gullycricket.backend.stats.controller;

import com.gullycricket.backend.matches.entity.MatchType;
import com.gullycricket.backend.stats.dto.*;
import com.gullycricket.backend.stats.enums.*;
import com.gullycricket.backend.stats.service.PlayerStatsService;
import com.gullycricket.backend.stats.service.TeamStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {

    private final PlayerStatsService playerStatsService;
    private final TeamStatsService teamStatsService;

    // =====================================================================
    // Player profile
    // =====================================================================

    @GetMapping("/player/{playerId}")
    public ResponseEntity<PlayerProfileDto> getPlayerProfile(
            @PathVariable String playerId,
            @RequestParam(required = false) String seasonId
    ) {
        return ResponseEntity.ok(playerStatsService.getPlayerProfile(playerId, seasonId));
    }

    // =====================================================================
    // Team profile
    // =====================================================================

    @GetMapping("/team/{teamId}")
    public ResponseEntity<TeamProfileDto> getTeamProfile(
            @PathVariable String teamId,
            @RequestParam(required = false) String seasonId
    ) {
        return ResponseEntity.ok(teamStatsService.getTeamProfile(teamId, seasonId));
    }

    // =====================================================================
    // Leaderboards
    // =====================================================================

    @GetMapping("/leaderboard/batting")
    public ResponseEntity<List<BattingStatsResponse>> getBattingLeaderboard(
            @RequestParam(required = false) String seasonId,
            @RequestParam(required = false) MatchType matchType,
            @RequestParam(required = false) String teamId,
            @RequestParam(required = false) String opponentTeamId,
            @RequestParam(required = false) Integer battingPosition,
            @RequestParam(required = false) Integer inningsNumber,
            @RequestParam(required = false) MatchResult result,
            @RequestParam(required = false, defaultValue = "RUNS") BattingSortBy sortBy,
            @RequestParam(required = false) Integer minInnings,
            @RequestParam(required = false) Integer limit
    ) {
        BattingStatsFilter filter = new BattingStatsFilter(seasonId, matchType, teamId, opponentTeamId, battingPosition, inningsNumber, result);
        return ResponseEntity.ok(playerStatsService.getBattingLeaderboard(filter, sortBy, minInnings, limit));
    }

    @GetMapping("/leaderboard/bowling")
    public ResponseEntity<List<BowlingStatsResponse>> getBowlingLeaderboard(
            @RequestParam(required = false) String seasonId,
            @RequestParam(required = false) MatchType matchType,
            @RequestParam(required = false) String teamId,
            @RequestParam(required = false) String opponentTeamId,
            @RequestParam(required = false) Integer inningsNumber,
            @RequestParam(required = false) MatchResult result,
            @RequestParam(required = false, defaultValue = "WICKETS") BowlingSortBy sortBy,
            @RequestParam(required = false) Integer minInnings,
            @RequestParam(required = false) Integer limit
    ) {
        BowlingStatsFilter filter = new BowlingStatsFilter(seasonId, matchType, teamId, opponentTeamId, inningsNumber, result);
        return ResponseEntity.ok(playerStatsService.getBowlingLeaderboard(filter, sortBy, minInnings, limit));
    }

    @GetMapping("/leaderboard/fielding")
    public ResponseEntity<List<FieldingAndMiscStatsResponse>> getFieldingLeaderboard(
            @RequestParam(required = false) String seasonId,
            @RequestParam(required = false) MatchType matchType,
            @RequestParam(required = false) String teamId,
            @RequestParam(required = false) String opponentTeamId,
            @RequestParam(required = false) Integer inningsNumber,
            @RequestParam(required = false) MatchResult result,
            @RequestParam(required = false, defaultValue = "DISMISSALS") FieldingSortBy sortBy,
            @RequestParam(required = false) Integer limit
    ) {
        FieldingAndMiscStatsFilter filter = new FieldingAndMiscStatsFilter(seasonId, matchType, teamId, opponentTeamId, inningsNumber, result);
        return ResponseEntity.ok(playerStatsService.getFieldingLeaderboard(filter, sortBy, limit));
    }

    @GetMapping("/leaderboard/teams")
    public ResponseEntity<List<TeamLeaderboardEntryDto>> getTeamLeaderboard(
            @RequestParam(required = false) String seasonId,
            @RequestParam(required = false, defaultValue = "MATCHES_WON") TeamSortBy sortBy,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(teamStatsService.getTeamLeaderboard(seasonId, sortBy, limit));
    }
}
