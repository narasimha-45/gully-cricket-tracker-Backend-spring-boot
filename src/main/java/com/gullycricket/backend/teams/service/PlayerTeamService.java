package com.gullycricket.backend.teams.service;

import com.gullycricket.backend.players.entity.Player;
import com.gullycricket.backend.seasons.entity.Season;
import com.gullycricket.backend.teams.DTOs.TeamSeasonPlayerDto;
import com.gullycricket.backend.teams.entity.PlayerTeam;
import com.gullycricket.backend.teams.entity.Team;
import com.gullycricket.backend.teams.reposistory.PlayerTeamRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PlayerTeamService {

    @Autowired
    private PlayerTeamRepository playerTeamRepository;

    public PlayerTeam savePlayerTeam(PlayerTeam playerTeam){
        return playerTeamRepository.save(playerTeam);
    }

    public List<PlayerTeam> saveListOfPlayerTeams(List<PlayerTeam> playerTeams) {
        log.info("Saving {} player teams", playerTeams.size());
        return playerTeamRepository.saveAll(playerTeams);
    }

    public boolean existsByPlayerAndTeamAndSeason(Player player, Team team, Season season) {
        return playerTeamRepository.existsByPlayerAndTeamAndSeason(player, team, season);
    }

    public Set<String> findExistingPlayerNamesByTeamAndSeason(Team team, Season season) {
        return playerTeamRepository.findByTeamAndSeason(team, season)
                .stream()
                .map(pt -> pt.getPlayer().getName())
                .collect(Collectors.toSet());
    }

    public List<TeamSeasonPlayerDto> getPlayersByTeamAndSeason(String teamId, String seasonId) {
        List<PlayerTeam> playerTeams = playerTeamRepository.getPlayersByTeam_IdAndSeason_Id(teamId, seasonId);
        return playerTeams.stream()
                .map(pt -> new TeamSeasonPlayerDto(
                        pt.getPlayer().getId(),
                        pt.getPlayer().getName()
                ))
                .toList();
    }
}
