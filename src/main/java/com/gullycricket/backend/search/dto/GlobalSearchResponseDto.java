package com.gullycricket.backend.search.dto;

import com.gullycricket.backend.players.dto.PlayerSearchSuggestionDto;
import com.gullycricket.backend.seasons.dto.SeasonSearchDto;
import com.gullycricket.backend.teams.dto.TeamSearchSuggestionDto;

import java.util.List;

public record GlobalSearchResponseDto(
        List<PlayerSearchSuggestionDto> players,
        List<TeamSearchSuggestionDto> teams,
        List<SeasonSearchDto> seasons
) {
}
