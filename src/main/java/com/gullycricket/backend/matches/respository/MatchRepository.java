package com.gullycricket.backend.matches.respository;

import com.gullycricket.backend.matches.entity.Match;
import com.gullycricket.backend.matches.entity.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MatchRepository extends JpaRepository<Match, String> {
    List<Match> findBySeason_Id(String seasonId);

    @Query("SELECT m FROM Match m WHERE m.status = :status AND (m.teamA.id = :teamId OR m.teamB.id = :teamId)")
    List<Match> findCompletedMatchesForTeam(@Param("teamId") String teamId, @Param("status") MatchStatus status);

    @Query("SELECT m FROM Match m WHERE m.status = :status AND m.season.id = :seasonId AND (m.teamA.id = :teamId OR m.teamB.id = :teamId)")
    List<Match> findCompletedMatchesForTeamAndSeason(@Param("teamId") String teamId, @Param("seasonId") String seasonId, @Param("status") MatchStatus status);

    List<Match> findByStatus(MatchStatus status);

    List<Match> findByStatusAndSeason_Id(MatchStatus status, String seasonId);
}
