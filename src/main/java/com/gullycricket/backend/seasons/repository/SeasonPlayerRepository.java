package com.gullycricket.backend.seasons.repository;

import com.gullycricket.backend.seasons.entity.SeasonPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface SeasonPlayerRepository extends JpaRepository<SeasonPlayer, String> {

    List<SeasonPlayer> findBySeasonId(String seasonId);

    @Query("SELECT sp.player.id FROM SeasonPlayer sp WHERE sp.season.id = :seasonId")
    Set<String> findPlayerIdsBySeasonId(@Param("seasonId") String seasonId);

    boolean existsByPlayerIdAndSeasonId(String playerId, String seasonId);
}