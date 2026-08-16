package com.gullycricket.backend.players.repository;

import com.gullycricket.backend.players.entity.PlayerMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlayerMatchRepository extends JpaRepository<PlayerMatch, String>, JpaSpecificationExecutor<PlayerMatch> {

    List<PlayerMatch> findByPlayer_Id(String playerId);

    List<PlayerMatch> findByPlayer_IdAndSeason_Id(String playerId, String seasonId);

    // One grouped query for however many players matched a search, instead of loading
    // each player's full playerMatches collection (or running one COUNT per player) —
    // this is what keeps player search fast regardless of how many matches a
    // long-career player has played.
    @Query("SELECT pm.player.id, COUNT(pm) FROM PlayerMatch pm WHERE pm.player.id IN :playerIds GROUP BY pm.player.id")
    List<Object[]> countMatchesByPlayerIds(@Param("playerIds") List<String> playerIds);
}
