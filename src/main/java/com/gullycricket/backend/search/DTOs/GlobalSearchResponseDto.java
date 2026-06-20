package com.gullycricket.backend.search.DTOs;

import com.gullycricket.backend.players.DTOs.PlayerSearchSuggestionDto;
import com.gullycricket.backend.seasons.DTOs.SeasonSearchDto;
import com.gullycricket.backend.teams.DTOs.TeamSearchSuggestionDto;

import java.util.List;

public record GlobalSearchResponseDto(
        List<PlayerSearchSuggestionDto> players,
        List<TeamSearchSuggestionDto> teams,
        List<SeasonSearchDto> seasons
) {
}
