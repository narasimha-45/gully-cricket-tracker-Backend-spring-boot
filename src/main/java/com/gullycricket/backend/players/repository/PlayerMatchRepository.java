package com.gullycricket.backend.players.repository;

import com.gullycricket.backend.players.entity.PlayerMatch;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlayerMatchRepository extends JpaRepository<PlayerMatch, String>, JpaSpecificationExecutor<PlayerMatch> {

    @EntityGraph(attributePaths = {"player", "match", "season", "teamRepresented", "oppositionTeam"})
    List<PlayerMatch> findByPlayer_Id(String playerId);

    @EntityGraph(attributePaths = {"player", "match", "season", "teamRepresented", "oppositionTeam"})
    List<PlayerMatch> findByPlayer_IdAndSeason_Id(String playerId, String seasonId);

    @Override
    @EntityGraph(attributePaths = {"player", "match", "season", "teamRepresented", "oppositionTeam"})
    List<PlayerMatch> findAll(Specification<PlayerMatch> spec);

    @Query("SELECT pm.player.id, COUNT(DISTINCT pm.match.id) FROM PlayerMatch pm WHERE pm.player.id IN :playerIds GROUP BY pm.player.id")
    List<Object[]> countMatchesByPlayerIds(@Param("playerIds") List<String> playerIds);
}
