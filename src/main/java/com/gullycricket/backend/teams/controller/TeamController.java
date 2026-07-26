package com.gullycricket.backend.teams.controller;

import com.gullycricket.backend.teams.DTOs.TeamSearchSuggestionDto;
import com.gullycricket.backend.teams.DTOs.TeamSeasonPlayerDto;
import com.gullycricket.backend.teams.service.PlayerTeamService;
import com.gullycricket.backend.teams.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
@RequestMapping("/teams")
@Tag(name = "Teams API", description = "Operations related to Teams")
public class TeamController {

    @Autowired
    private TeamService teamService;

    @Autowired
    private PlayerTeamService playerTeamService;

    @GetMapping("/search")
    @Operation(
            summary = "Get all teams matching the query",
            description = "Return all teams matching the query"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved all teams for the query"
    )
    public ResponseEntity<List<TeamSearchSuggestionDto>> searchTeams(@RequestParam String query){
        log.info("received query to search teams: {}",query);
        return ResponseEntity.ok(teamService.searchTeam(query));
    }

    @GetMapping("/season-player")
    @Operation(
            summary = "Get team season player",
            description = "Return the player information for a team in a specific season"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved team season player"
    )
    public ResponseEntity<List<TeamSeasonPlayerDto>> getTeamSeasonPlayer(@RequestParam String teamId, @RequestParam String seasonId) {
        log.info("received request to get team season player for team: {} and season: {}", teamId, seasonId);
        List<TeamSeasonPlayerDto> teamSeasonPlayerDtos = playerTeamService.getPlayersByTeamAndSeason(teamId,seasonId);
        return ResponseEntity.ok(teamSeasonPlayerDtos);
    }
}
