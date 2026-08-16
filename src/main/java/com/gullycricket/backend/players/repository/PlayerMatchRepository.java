package com.gullycricket.backend.players.repository;

import com.gullycricket.backend.players.entity.PlayerMatch;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * Write-side repository for per-innings player projections.
 *
 * <p>Leaderboard/profile reads intentionally do not live here. They use the
 * read-optimized JDBC repositories under {@code stats.repository} so a stats
 * request cannot accidentally hydrate Match.matchData JSONB through JPA.</p>
 */
public interface PlayerMatchRepository extends JpaRepository<PlayerMatch, String> {

}
