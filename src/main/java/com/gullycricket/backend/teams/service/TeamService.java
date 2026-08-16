package com.gullycricket.backend.teams.service;


import com.gullycricket.backend.teams.DTOs.TeamSearchSuggestionDto;
import com.gullycricket.backend.teams.entity.Team;
import com.gullycricket.backend.teams.reposistory.TeamRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class TeamService {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private PlayerTeamService playerTeamService;

    public List<TeamSearchSuggestionDto> searchTeam(String query){
        log.info("searching for the teams having {}",query);

        List<TeamSearchSuggestionDto> teams = teamRepository.findByTeamNameContainingIgnoreCase(query)
                .stream()
                .map(team -> new TeamSearchSuggestionDto(
                        team.getId(),
                        team.getTeamName()
                ))
                .toList();

        log.info("Response of teams: {}",teams);

        return teams;
    }

    public Team getTeamByName(String name){
        return teamRepository.findByTeamName(name);
    }

    public Team saveTeam(Team team){
        return teamRepository.save(team);
    }

    public List<TeamSearchSuggestionDto> getAllTeams() {
        List<Team> teams = teamRepository.findAll();

        return teams.stream().map(
                team -> new TeamSearchSuggestionDto(
                        team.getId(),
                        team.getTeamName()
                )
        ).toList();

    }

    public List<TeamSearchSuggestionDto> getTeamsBySeasonId(String seasonId) {
        return teamRepository
                .findDistinctBySeasonsPlayed_Id(seasonId)
                .stream()
                .map(team -> new TeamSearchSuggestionDto(
                        team.getId(),
                        team.getTeamName()
                ))
                .toList();
    }
}
