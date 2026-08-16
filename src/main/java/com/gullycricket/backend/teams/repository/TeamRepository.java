package com.gullycricket.backend.teams.repository;

import com.gullycricket.backend.teams.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team,String> {
    List<Team> findTop10ByTeamNameContainingIgnoreCaseOrderByTeamNameAsc(String name);
    Team findByTeamName(String name);
    List<Team> findByTeamNameIn(List<String> names);
    List<Team> findDistinctBySeasonsPlayed_Id(String seasonId);

    @Modifying(flushAutomatically = true)
    @Query(value = "INSERT INTO team_season(team_id, season_id) VALUES (:teamId, :seasonId) ON CONFLICT DO NOTHING", nativeQuery = true)
    int addSeasonMembership(@Param("teamId") String teamId, @Param("seasonId") String seasonId);
}
