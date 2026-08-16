package com.gullycricket.backend.search.service;

import com.gullycricket.backend.players.dto.PlayerSearchSuggestionDto;
import com.gullycricket.backend.players.service.PlayerService;
import com.gullycricket.backend.search.dto.GlobalSearchResponseDto;
import com.gullycricket.backend.seasons.dto.SeasonSearchDto;
import com.gullycricket.backend.seasons.service.SeasonService;
import com.gullycricket.backend.teams.dto.TeamSearchSuggestionDto;
import com.gullycricket.backend.teams.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GlobalSearchService {

    private final PlayerService playerService;
    private final SeasonService seasonService;
    private final TeamService teamService;

    public GlobalSearchResponseDto globalSearch(String query) {
        String normalized = query == null ? "" : query.trim();
        if (normalized.length() < 2) {
            return new GlobalSearchResponseDto(List.of(), List.of(), List.of());
        }

        List<PlayerSearchSuggestionDto> players = playerService.searchPlayers(normalized);
        List<TeamSearchSuggestionDto> teams = teamService.searchTeam(normalized);
        List<SeasonSearchDto> seasons = seasonService.searchSeasons(normalized);
        return new GlobalSearchResponseDto(players, teams, seasons);
    }
}
