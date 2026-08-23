package com.gullycricket.backend.stats.repository;

import com.gullycricket.backend.config.DbQueryTimer;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PlayerProfileReadRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final DbQueryTimer queryTimer;

    public List<PlayerStatReadRow> findRows(String playerId, String seasonId) {
        MapSqlParameterSource params = new MapSqlParameterSource("playerId", playerId);
        // Participation is the source of truth for whether a player played a match.
        // player_matches is an innings/activity projection and legitimately has no row
        // when a squad member neither bats, bowls nor records a fielding event.
        StringBuilder sql = new StringBuilder("""
                SELECT
                    mpp.player_id,
                    p.name AS player_name,
                    mpp.match_id,
                    mpp.season_id,
                    s.season_name,
                    mpp.team_represented_id AS team_id,
                    tr.team_name,
                    mpp.opposition_team_id,
                    ot.team_name AS opponent_team_name,
                    pm.innings_number,
                    mpp.match_won,
                    mpp.player_of_the_match,
                    COALESCE(pm.batted, FALSE) AS batted,
                    pm.batting_position,
                    COALESCE(pm.runs_scored, 0) AS runs_scored,
                    COALESCE(pm.balls_faced, 0) AS balls_faced,
                    COALESCE(pm.fours_hit, 0) AS fours_hit,
                    COALESCE(pm.sixes_hit, 0) AS sixes_hit,
                    COALESCE(pm.out, FALSE) AS out,
                    COALESCE(pm.dot_balls_played, 0) AS dot_balls_played,
                    COALESCE(pm.bowled, FALSE) AS bowled,
                    COALESCE(pm.wickets_taken, 0) AS wickets_taken,
                    COALESCE(pm.balls_bowled, 0) AS balls_bowled,
                    COALESCE(pm.runs_conceded, 0) AS runs_conceded,
                    COALESCE(pm.maidens_bowled, 0) AS maidens_bowled,
                    COALESCE(pm.dot_balls_bowled, 0) AS dot_balls_bowled,
                    COALESCE(pm.catches_taken, 0) AS catches_taken,
                    COALESCE(pm.run_outs, 0) AS run_outs,
                    COALESCE(pm.stumpings, 0) AS stumpings,
                    COALESCE(m.is_match_tied, FALSE) AS match_tied,
                    COALESCE(m.is_match_drawn, FALSE) AS match_drawn,
                    m.winner_team_id,
                    m.completed_at
                FROM match_player_participation mpp
                JOIN players p ON p.id = mpp.player_id
                JOIN matches m ON m.id = mpp.match_id
                JOIN seasons s ON s.id = mpp.season_id
                JOIN teams tr ON tr.id = mpp.team_represented_id
                JOIN teams ot ON ot.id = mpp.opposition_team_id
                LEFT JOIN player_matches pm
                    ON pm.player_id = mpp.player_id
                   AND pm.match_id = mpp.match_id
                   AND pm.team_represented_id = mpp.team_represented_id
                WHERE mpp.player_id = :playerId
                """);
        if (seasonId != null && !seasonId.isBlank()) {
            sql.append(" AND mpp.season_id = :seasonId");
            params.addValue("seasonId", seasonId.trim());
        }
        sql.append(" ORDER BY m.completed_at DESC NULLS LAST, pm.innings_number DESC NULLS LAST");
        return queryTimer.record("stats.playerProfileRows", () -> jdbc.query(sql.toString(), params, this::mapRow));
    }

    private PlayerStatReadRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new PlayerStatReadRow(
                rs.getString("player_id"),
                rs.getString("player_name"),
                rs.getString("match_id"),
                rs.getString("season_id"),
                rs.getString("season_name"),
                rs.getString("team_id"),
                rs.getString("team_name"),
                rs.getString("opposition_team_id"),
                rs.getString("opponent_team_name"),
                getNullableInt(rs, "innings_number"),
                rs.getBoolean("match_won"),
                rs.getBoolean("player_of_the_match"),
                rs.getBoolean("batted"),
                getNullableInt(rs, "batting_position"),
                rs.getInt("runs_scored"),
                rs.getInt("balls_faced"),
                rs.getInt("fours_hit"),
                rs.getInt("sixes_hit"),
                rs.getBoolean("out"),
                rs.getInt("dot_balls_played"),
                rs.getBoolean("bowled"),
                rs.getInt("wickets_taken"),
                rs.getInt("balls_bowled"),
                rs.getInt("runs_conceded"),
                rs.getInt("maidens_bowled"),
                rs.getInt("dot_balls_bowled"),
                rs.getInt("catches_taken"),
                rs.getInt("run_outs"),
                rs.getInt("stumpings"),
                rs.getBoolean("match_tied"),
                rs.getBoolean("match_drawn"),
                rs.getString("winner_team_id"),
                rs.getTimestamp("completed_at") == null ? null : rs.getTimestamp("completed_at").toLocalDateTime()
        );
    }

    private Integer getNullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }
}
