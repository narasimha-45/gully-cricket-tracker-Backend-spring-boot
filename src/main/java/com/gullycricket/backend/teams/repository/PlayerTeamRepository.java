package com.gullycricket.backend.teams.repository;

import com.gullycricket.backend.players.entity.Player;
import com.gullycricket.backend.seasons.entity.Season;
import com.gullycricket.backend.teams.entity.PlayerTeam;
import com.gullycricket.backend.teams.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerTeamRepository extends JpaRepository<PlayerTeam, String> {

    boolean existsByPlayerAndTeamAndSeason(Player player, Team team, Season season);

    List<PlayerTeam> findByTeamAndSeason(Team team, Season season);

    List<PlayerTeam> findBySeason_IdAndActiveTrue(String seasonId);

    List<PlayerTeam> getPlayersByTeam_IdAndSeason_Id(String teamId, String seasonId);
}
