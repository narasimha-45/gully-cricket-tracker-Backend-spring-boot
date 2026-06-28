package com.gullycricket.backend.search.service;

import com.gullycricket.backend.players.DTOs.PlayerSearchSuggestionDto;
import com.gullycricket.backend.players.service.PlayerService;
import com.gullycricket.backend.search.DTOs.GlobalSearchResponseDto;
import com.gullycricket.backend.seasons.DTOs.SeasonSearchDto;
import com.gullycricket.backend.seasons.service.SeasonService;
import com.gullycricket.backend.teams.DTOs.TeamSearchSuggestionDto;
import com.gullycricket.backend.teams.service.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GlobalSearchService {

    @Autowired
    private PlayerService playerService;

    @Autowired
    private SeasonService seasonService;

    @Autowired
    private TeamService teamService;

    public GlobalSearchResponseDto globalSearch(String query){
        List<PlayerSearchSuggestionDto> players = playerService.searchPlayers(query);
        List<TeamSearchSuggestionDto> teams = teamService.searchTeam(query);
        List<SeasonSearchDto> seasons = seasonService.searchSeasons(query);

        return new GlobalSearchResponseDto(
                players,
                teams,
                seasons
        );
    }
}
