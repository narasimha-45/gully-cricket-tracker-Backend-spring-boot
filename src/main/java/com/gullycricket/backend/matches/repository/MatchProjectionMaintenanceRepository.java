package com.gullycricket.backend.matches.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Low-level maintenance operations for data that is derived from matches.
 *
 * <p>The matches table (especially match_data JSONB) is intentionally not touched here.
 * Rebuild services delete these projections and replay the stored match payloads.</p>
 */
@Repository
@RequiredArgsConstructor
public class MatchProjectionMaintenanceRepository {

    private static final long PROJECTION_WRITE_LOCK_KEY = 6_842_091_337L;

    private final JdbcTemplate jdbcTemplate;

    /**
     * Serializes match creation/deletion/rebuild transactions. PostgreSQL releases this
     * automatically when the surrounding transaction commits or rolls back.
     */
    public void acquireProjectionWriteLock() {
        jdbcTemplate.execute("SELECT pg_advisory_xact_lock(" + PROJECTION_WRITE_LOCK_KEY + ")");
    }

    public void deleteMatchProjections(String matchId) {
        jdbcTemplate.update("DELETE FROM player_rivalries WHERE match_id = ?", matchId);
        jdbcTemplate.update("DELETE FROM player_partnerships WHERE match_id = ?", matchId);
        jdbcTemplate.update("DELETE FROM player_matches WHERE match_id = ?", matchId);
        jdbcTemplate.update("DELETE FROM match_player_participation WHERE match_id = ?", matchId);
        jdbcTemplate.update("DELETE FROM match_innings_summary WHERE match_id = ?", matchId);
    }

    public void deleteSeasonProjections(String seasonId) {
        jdbcTemplate.update("DELETE FROM player_rivalries WHERE season_id = ?", seasonId);
        jdbcTemplate.update("DELETE FROM player_partnerships WHERE season_id = ?", seasonId);
        jdbcTemplate.update("DELETE FROM player_matches WHERE season_id = ?", seasonId);
        jdbcTemplate.update("DELETE FROM match_player_participation WHERE season_id = ?", seasonId);
        jdbcTemplate.update(
                "DELETE FROM match_innings_summary WHERE match_id IN (SELECT id FROM matches WHERE season_id = ?)",
                seasonId);

        // These are season-wide projections. Replaying all matches in the season recreates
        // only memberships that are actually supported by a remaining match.
        jdbcTemplate.update("DELETE FROM player_teams WHERE season_id = ?", seasonId);
        jdbcTemplate.update("DELETE FROM season_players WHERE season_id = ?", seasonId);
        jdbcTemplate.update("DELETE FROM team_season WHERE season_id = ?", seasonId);
    }

    public void deleteAllProjections() {
        jdbcTemplate.update("DELETE FROM player_rivalries");
        jdbcTemplate.update("DELETE FROM player_partnerships");
        jdbcTemplate.update("DELETE FROM player_matches");
        jdbcTemplate.update("DELETE FROM match_player_participation");
        jdbcTemplate.update("DELETE FROM match_innings_summary");
        jdbcTemplate.update("DELETE FROM player_teams");
        jdbcTemplate.update("DELETE FROM season_players");
        jdbcTemplate.update("DELETE FROM team_season");
    }
}
