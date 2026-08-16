package com.gullycricket.backend.teams.controller;

import com.gullycricket.backend.teams.dto.TeamSearchSuggestionDto;
import com.gullycricket.backend.teams.dto.TeamSeasonPlayerDto;
import com.gullycricket.backend.teams.service.PlayerTeamService;
import com.gullycricket.backend.teams.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
@Tag(name = "Teams API", description = "Operations related to Teams")
public class TeamController {

    private final TeamService teamService;
    private final PlayerTeamService playerTeamService;

    @GetMapping("/search")
    @Operation(summary = "Search teams", description = "Return up to 10 teams matching the query")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved matching teams")
    public ResponseEntity<List<TeamSearchSuggestionDto>> searchTeams(@RequestParam String query) {
        return ResponseEntity.ok(teamService.searchTeam(query));
    }

    // Existing endpoint retained for frontend compatibility.
    @GetMapping("/get-teams")
    public ResponseEntity<List<TeamSearchSuggestionDto>> getTeams(
            @RequestParam(defaultValue = "All") String seasonId) {
        List<TeamSearchSuggestionDto> teams = "All".equalsIgnoreCase(seasonId.trim())
                ? teamService.getAllTeams()
                : teamService.getTeamsBySeasonId(seasonId.trim());
        return ResponseEntity.ok(teams);
    }

    @GetMapping("/season-player")
    @Operation(summary = "Get team roster for a season")
    public ResponseEntity<List<TeamSeasonPlayerDto>> getTeamSeasonPlayer(
            @RequestParam String teamId,
            @RequestParam String seasonId) {
        return ResponseEntity.ok(playerTeamService.getPlayersByTeamAndSeason(teamId, seasonId));
    }
}
