package com.gullycricket.backend.teams.service;

import com.gullycricket.backend.common.util.NameNormalizer;
import com.gullycricket.backend.teams.dto.TeamSearchSuggestionDto;
import com.gullycricket.backend.teams.entity.Team;
import com.gullycricket.backend.teams.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;

    @Transactional(readOnly = true)
    public List<TeamSearchSuggestionDto> searchTeam(String query) {
        String normalized = normalizeSearch(query);
        if (normalized.length() < 2) {
            return List.of();
        }
        return teamRepository.findTop10ByTeamNameContainingIgnoreCaseOrderByTeamNameAsc(normalized)
                .stream()
                .map(team -> new TeamSearchSuggestionDto(team.getId(), team.getTeamName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Team getTeamByName(String name) {
        return teamRepository.findByTeamName(NameNormalizer.normalize(name));
    }

    public Team saveTeam(Team team) {
        team.setTeamName(NameNormalizer.normalize(team.getTeamName()));
        return teamRepository.save(team);
    }

    @Transactional(readOnly = true)
    public List<TeamSearchSuggestionDto> getAllTeams() {
        return teamRepository.findAll().stream()
                .map(team -> new TeamSearchSuggestionDto(team.getId(), team.getTeamName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeamSearchSuggestionDto> getTeamsBySeasonId(String seasonId) {
        return teamRepository.findDistinctBySeasonsPlayed_Id(seasonId).stream()
                .map(team -> new TeamSearchSuggestionDto(team.getId(), team.getTeamName()))
                .toList();
    }

    private String normalizeSearch(String query) {
        String value = NameNormalizer.normalize(query);
        return value == null ? "" : value;
    }
}
