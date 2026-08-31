package com.gullycricket.backend.matches.repository;

import com.gullycricket.backend.matches.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Match aggregate write repository. High-traffic summary/statistics endpoints
 * use compact JDBC read models so they never select the large match_data JSONB
 * column by accident.
 */
public interface MatchRepository extends JpaRepository<Match, String> {
    Optional<Match> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT m.id FROM Match m WHERE m.season.id = :seasonId ORDER BY m.completedAt ASC, m.id ASC")
    List<String> findIdsBySeasonIdForReplay(@Param("seasonId") String seasonId);

    @Query("SELECT m.id FROM Match m ORDER BY m.completedAt ASC, m.id ASC")
    List<String> findAllIdsForReplay();
}

