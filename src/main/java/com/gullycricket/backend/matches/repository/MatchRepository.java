package com.gullycricket.backend.matches.repository;

import com.gullycricket.backend.matches.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Match aggregate write repository. High-traffic summary/statistics endpoints
 * use compact JDBC read models so they never select the large match_data JSONB
 * column by accident.
 */
public interface MatchRepository extends JpaRepository<Match, String> {
    Optional<Match> findByIdempotencyKey(String idempotencyKey);
}
