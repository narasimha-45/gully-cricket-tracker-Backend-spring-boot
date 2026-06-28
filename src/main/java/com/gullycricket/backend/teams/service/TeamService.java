package com.gullycricket.backend.teams.service;


import com.gullycricket.backend.teams.DTOs.TeamSearchSuggestionDto;
import com.gullycricket.backend.teams.entity.PlayerTeam;
import com.gullycricket.backend.teams.entity.Team;
import com.gullycricket.backend.teams.reposistory.PlayerTeamRepository;
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

}
