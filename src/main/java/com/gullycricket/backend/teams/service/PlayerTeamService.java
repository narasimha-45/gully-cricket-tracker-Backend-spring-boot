package com.gullycricket.backend.teams.service;

import com.gullycricket.backend.players.entity.Player;
import com.gullycricket.backend.seasons.entity.Season;
import com.gullycricket.backend.teams.entity.PlayerTeam;
import com.gullycricket.backend.teams.entity.Team;
import com.gullycricket.backend.teams.reposistory.PlayerTeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlayerTeamService {

    @Autowired
    private PlayerTeamRepository playerTeamRepository;

    public PlayerTeam savePlayerTeam(PlayerTeam playerTeam){
        return playerTeamRepository.save(playerTeam);
    }

    public boolean existsByPlayerAndTeamAndSeason(
            Player player,
            Team team,
            Season season
    ) {
        return playerTeamRepository
                .existsByPlayerAndTeamAndSeason(
                        player,
                        team,
                        season
                );
    }

    public List<PlayerTeam> saveListOfPlayerTeams(List<PlayerTeam> playerTeams){
        return playerTeamRepository.saveAll(playerTeams);
    }
}
