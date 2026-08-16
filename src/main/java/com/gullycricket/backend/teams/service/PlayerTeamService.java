package com.gullycricket.backend.teams.service;

import com.gullycricket.backend.players.entity.Player;
import com.gullycricket.backend.seasons.entity.Season;
import com.gullycricket.backend.teams.dto.TeamSeasonPlayerDto;
import com.gullycricket.backend.teams.entity.PlayerTeam;
import com.gullycricket.backend.teams.entity.Team;
import com.gullycricket.backend.teams.repository.PlayerTeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerTeamService {

    private final PlayerTeamRepository playerTeamRepository;

    public PlayerTeam savePlayerTeam(PlayerTeam playerTeam) {
        return playerTeamRepository.save(playerTeam);
    }

    public List<PlayerTeam> saveListOfPlayerTeams(List<PlayerTeam> playerTeams) {
        log.debug("Saving {} player-team memberships", playerTeams.size());
        return playerTeamRepository.saveAll(playerTeams);
    }

    @Transactional(readOnly = true)
    public boolean existsByPlayerAndTeamAndSeason(Player player, Team team, Season season) {
        return playerTeamRepository.existsByPlayerAndTeamAndSeason(player, team, season);
    }

    @Transactional(readOnly = true)
    public Set<String> findExistingPlayerNamesByTeamAndSeason(Team team, Season season) {
        return playerTeamRepository.findPlayerNamesByTeamAndSeason(team.getId(), season.getId());
    }

    @Transactional(readOnly = true)
    public List<TeamSeasonPlayerDto> getPlayersByTeamAndSeason(String teamId, String seasonId) {
        return playerTeamRepository.findRoster(teamId, seasonId);
    }
}
