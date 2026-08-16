package com.gullycricket.backend.teams.service;

import com.gullycricket.backend.common.util.NameNormalizer;
import com.gullycricket.backend.config.CacheNames;
import com.gullycricket.backend.teams.dto.TeamSearchSuggestionDto;
import com.gullycricket.backend.teams.entity.Team;
import com.gullycricket.backend.teams.repository.TeamRepository;
import com.gullycricket.backend.teams.repository.read.TeamReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamReadRepository teamReadRepository;

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

    @Transactional(readOnly = true)
    public List<Team> getTeamsByNames(List<String> names) {
        List<String> canonicalNames = names.stream().map(NameNormalizer::normalize).distinct().toList();
        return canonicalNames.isEmpty() ? List.of() : teamRepository.findByTeamNameIn(canonicalNames);
    }

    @CacheEvict(value = {CacheNames.ALL_TEAMS, CacheNames.SEASON_TEAMS}, allEntries = true)
    public Team saveTeam(Team team) {
        team.setTeamName(NameNormalizer.normalize(team.getTeamName()));
        return teamRepository.save(team);
    }


    @Transactional
    @CacheEvict(value = CacheNames.SEASON_TEAMS, allEntries = true)
    public void ensureTeamInSeason(String teamId, String seasonId) {
        teamRepository.addSeasonMembership(teamId, seasonId);
    }

    @Cacheable(value = CacheNames.ALL_TEAMS, sync = true)
    public List<TeamSearchSuggestionDto> getAllTeams() {
        return teamReadRepository.findAll();
    }

    @Cacheable(value = CacheNames.SEASON_TEAMS, key = "#seasonId", sync = true)
    public List<TeamSearchSuggestionDto> getTeamsBySeasonId(String seasonId) {
        return teamReadRepository.findBySeasonId(seasonId);
    }

    private String normalizeSearch(String query) {
        String value = NameNormalizer.normalize(query);
        return value == null ? "" : value;
    }
}
