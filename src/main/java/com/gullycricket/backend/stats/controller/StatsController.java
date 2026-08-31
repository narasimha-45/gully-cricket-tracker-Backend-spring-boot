package com.gullycricket.backend.stats.controller;

import com.gullycricket.backend.matches.entity.MatchType;
import com.gullycricket.backend.stats.dto.*;
import com.gullycricket.backend.stats.enums.*;
import com.gullycricket.backend.stats.service.PlayerStatsService;
import com.gullycricket.backend.stats.service.TeamStatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stats")
public class StatsController {

    private final PlayerStatsService playerStatsService;
    private final TeamStatsService teamStatsService;

    public StatsController(PlayerStatsService playerStatsService, TeamStatsService teamStatsService) {
        this.playerStatsService = playerStatsService;
        this.teamStatsService = teamStatsService;
    }

    // =====================================================================
    // Player profile
    // =====================================================================

    @GetMapping("/player/{playerId}")
    public ResponseEntity<PlayerProfileDto> getPlayerProfile(@PathVariable String playerId, @RequestParam(required = false) String seasonId, @RequestParam(required = false) MatchType matchType) {
        return ResponseEntity.ok(playerStatsService.getPlayerProfile(playerId, seasonId, matchType));
    }

    @GetMapping("/player/compare")
    public ResponseEntity<PlayerComparisonDto> comparePlayers(@RequestParam String player1Id, @RequestParam String player2Id, @RequestParam(required = false) String seasonId, @RequestParam(required = false) MatchType matchType, @RequestParam(required = false) String teamId, @RequestParam(required = false) String opponentTeamId, @RequestParam(required = false) MatchResult result,

                                                              @RequestParam(required = false) Integer battingInningsNumber, @RequestParam(required = false) Integer battingPosition, @RequestParam(required = false) Integer minBallsFaced,

                                                              @RequestParam(required = false) Integer bowlingInningsNumber, @RequestParam(required = false) Integer minOversBowled) {
        PlayerComparisonFilter filter = new PlayerComparisonFilter(seasonId, matchType, teamId, opponentTeamId, result, battingInningsNumber, battingPosition, minBallsFaced, bowlingInningsNumber, minOversBowled);

        return ResponseEntity.ok(playerStatsService.comparePlayers(player1Id, player2Id, filter));
    }

    @GetMapping("/leaderboard/partnerships/innings")
    public ResponseEntity<List<PartnershipInningsDto>> getPartnerships(@RequestParam(required = false) String seasonId, @RequestParam(required = false) MatchType matchType, @RequestParam(required = false) String teamId, @RequestParam(required = false) String opponentTeamId, @RequestParam(required = false) Integer inningsNumber, @RequestParam(required = false) MatchResult result, @RequestParam(required = false) Integer partnershipNumber, @RequestParam(required = false) String playerId, @RequestParam(required = false) String partnerId, @RequestParam(required = false) Boolean battingFirst, @RequestParam(required = false) Integer limit) {
        PartnershipStatsFilter filter = new PartnershipStatsFilter(seasonId, matchType, teamId, opponentTeamId, inningsNumber, result, partnershipNumber, playerId, partnerId, battingFirst);
        return ResponseEntity.ok(playerStatsService.getPartnershipInnings(filter, limit));
    }

    @GetMapping("/leaderboard/partnerships/aggregated")
    public ResponseEntity<List<PartnershipStatsResponse>> getPartnershipsAggregated(@RequestParam(required = false) String seasonId, @RequestParam(required = false) MatchType matchType, @RequestParam(required = false) String teamId, @RequestParam(required = false) String opponentTeamId, @RequestParam(required = false) Integer inningsNumber, @RequestParam(required = false) MatchResult result, @RequestParam(required = false) Integer partnershipNumber, @RequestParam(required = false) String playerId, @RequestParam(required = false) String partnerId, @RequestParam(required = false) Boolean battingFirst, @RequestParam(required = false) Integer limit) {
        PartnershipStatsFilter filter = new PartnershipStatsFilter(seasonId, matchType, teamId, opponentTeamId, inningsNumber, result, partnershipNumber, playerId, partnerId, battingFirst);
        return ResponseEntity.ok(playerStatsService.getPartnershipAggregatedStats(filter, limit));
    }

    @GetMapping("/partnerships/history")
    public ResponseEntity<List<PartnershipInningsDto>> getPartnershipHistory(@RequestParam String playerId, @RequestParam String partnerId, @RequestParam(required = false) String seasonId, @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(playerStatsService.getPartnershipHistory(playerId, partnerId, seasonId, limit));
    }

    @GetMapping("/rivalries")
    public ResponseEntity<List<RivalryStatsResponse>> getRivalries(@RequestParam(required = false) String seasonId, @RequestParam(required = false) MatchType matchType, @RequestParam(required = false) String teamId, @RequestParam(required = false) String opponentTeamId, @RequestParam(required = false) Integer inningsNumber, @RequestParam(required = false) MatchResult matchResult, @RequestParam(required = false) String batsmanId, @RequestParam(required = false) String bowlerId, @RequestParam(required = false) Integer minBallsFaced, @RequestParam(required = false) Integer minRuns, @RequestParam(required = false) Integer minDismissals, @RequestParam(required = false) Integer limit) {
        RivalryStatsFilter filter = new RivalryStatsFilter(seasonId, matchType, teamId, opponentTeamId, inningsNumber, matchResult, batsmanId, bowlerId, minBallsFaced, minRuns, minDismissals);
        return ResponseEntity.ok(playerStatsService.getRivalryStats(filter, limit));
    }

    // =====================================================================
    // Team profile
    // =====================================================================

    @GetMapping("/team/{teamId}")
    public ResponseEntity<TeamProfileDto> getTeamProfile(@PathVariable String teamId, @RequestParam(required = false) String seasonId, @RequestParam(required = false) MatchType matchType) {
        return ResponseEntity.ok(teamStatsService.getTeamProfile(teamId, seasonId, matchType));
    }

    // =====================================================================
    // Leaderboards
    // =====================================================================

    @GetMapping("/leaderboard/batting")
    public ResponseEntity<List<BattingStatsResponse>> getBattingLeaderboard(@RequestParam(required = false) String seasonId, @RequestParam(required = false) MatchType matchType, @RequestParam(required = false) String teamId, @RequestParam(required = false) String opponentTeamId, @RequestParam(required = false) Integer battingPosition, @RequestParam(required = false) Integer inningsNumber, @RequestParam(required = false) MatchResult result, @RequestParam(required = false, defaultValue = "RUNS") BattingSortBy sortBy, @RequestParam(required = false) Integer minInnings, @RequestParam(required = false) Integer limit) {
        BattingStatsFilter filter = new BattingStatsFilter(seasonId, matchType, teamId, opponentTeamId, battingPosition, inningsNumber, result);
        return ResponseEntity.ok(playerStatsService.getBattingLeaderboard(filter, sortBy, minInnings, limit));
    }

    @GetMapping("/leaderboard/bowling")
    public ResponseEntity<List<BowlingStatsResponse>> getBowlingLeaderboard(@RequestParam(required = false) String seasonId, @RequestParam(required = false) MatchType matchType, @RequestParam(required = false) String teamId, @RequestParam(required = false) String opponentTeamId, @RequestParam(required = false) Integer inningsNumber, @RequestParam(required = false) MatchResult result, @RequestParam(required = false, defaultValue = "WICKETS") BowlingSortBy sortBy, @RequestParam(required = false) Integer minInnings, @RequestParam(required = false) Integer limit) {
        BowlingStatsFilter filter = new BowlingStatsFilter(seasonId, matchType, teamId, opponentTeamId, inningsNumber, result);
        return ResponseEntity.ok(playerStatsService.getBowlingLeaderboard(filter, sortBy, minInnings, limit));
    }

    @GetMapping("/leaderboard/fielding")
    public ResponseEntity<List<FieldingAndMiscStatsResponse>> getFieldingLeaderboard(@RequestParam(required = false) String seasonId, @RequestParam(required = false) MatchType matchType, @RequestParam(required = false) String teamId, @RequestParam(required = false) String opponentTeamId, @RequestParam(required = false) Integer inningsNumber, @RequestParam(required = false) MatchResult result, @RequestParam(required = false, defaultValue = "DISMISSALS") FieldingSortBy sortBy, @RequestParam(required = false) Integer limit) {
        FieldingAndMiscStatsFilter filter = new FieldingAndMiscStatsFilter(seasonId, matchType, teamId, opponentTeamId, inningsNumber, result);
        return ResponseEntity.ok(playerStatsService.getFieldingLeaderboard(filter, sortBy, limit));
    }

    @GetMapping("/leaderboard/teams")
    public ResponseEntity<List<TeamLeaderboardEntryDto>> getTeamLeaderboard(@RequestParam(required = false) String seasonId, @RequestParam(required = false) MatchType matchType, @RequestParam(required = false, defaultValue = "MATCHES_WON") TeamSortBy sortBy, @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(teamStatsService.getTeamLeaderboard(seasonId, matchType, sortBy, limit));
    }
}
