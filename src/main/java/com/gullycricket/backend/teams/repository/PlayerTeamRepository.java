package com.gullycricket.backend.teams.repository;

import com.gullycricket.backend.players.entity.Player;
import com.gullycricket.backend.seasons.entity.Season;
import com.gullycricket.backend.teams.dto.TeamSeasonPlayerDto;
import com.gullycricket.backend.teams.entity.PlayerTeam;
import com.gullycricket.backend.teams.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface PlayerTeamRepository extends JpaRepository<PlayerTeam, String> {

    boolean existsByPlayerAndTeamAndSeason(Player player, Team team, Season season);

    @Query("SELECT p.name FROM PlayerTeam pt JOIN pt.player p WHERE pt.team.id = :teamId AND pt.season.id = :seasonId")
    Set<String> findPlayerNamesByTeamAndSeason(@Param("teamId") String teamId, @Param("seasonId") String seasonId);

    @Query("SELECT new com.gullycricket.backend.teams.dto.TeamSeasonPlayerDto(p.id, p.name) " +
            "FROM PlayerTeam pt JOIN pt.player p " +
            "WHERE pt.team.id = :teamId AND pt.season.id = :seasonId AND pt.active = true " +
            "ORDER BY p.name")
    List<TeamSeasonPlayerDto> findRoster(@Param("teamId") String teamId, @Param("seasonId") String seasonId);
}
