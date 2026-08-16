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
        StringBuilder sql = new StringBuilder("""
                SELECT
                    pm.player_id,
                    p.name AS player_name,
                    pm.match_id,
                    pm.season_id,
                    s.season_name,
                    pm.team_represented_id AS team_id,
                    tr.team_name,
                    pm.opposition_team_id,
                    ot.team_name AS opponent_team_name,
                    pm.innings_number,
                    pm.match_won,
                    pm.player_of_the_match,
                    pm.batted,
                    pm.batting_position,
                    pm.runs_scored,
                    pm.balls_faced,
                    pm.fours_hit,
                    pm.sixes_hit,
                    pm.out,
                    pm.dot_balls_played,
                    pm.bowled,
                    pm.wickets_taken,
                    pm.balls_bowled,
                    pm.runs_conceded,
                    pm.maidens_bowled,
                    pm.dot_balls_bowled,
                    pm.catches_taken,
                    pm.run_outs,
                    pm.stumpings,
                    COALESCE(m.is_match_tied, FALSE) AS match_tied,
                    COALESCE(m.is_match_drawn, FALSE) AS match_drawn,
                    m.winner_team_id,
                    m.completed_at
                FROM player_matches pm
                JOIN players p ON p.id = pm.player_id
                JOIN matches m ON m.id = pm.match_id
                JOIN seasons s ON s.id = pm.season_id
                JOIN teams tr ON tr.id = pm.team_represented_id
                JOIN teams ot ON ot.id = pm.opposition_team_id
                WHERE pm.player_id = :playerId
                """);
        if (seasonId != null && !seasonId.isBlank()) {
            sql.append(" AND pm.season_id = :seasonId");
            params.addValue("seasonId", seasonId.trim());
        }
        sql.append(" ORDER BY m.completed_at DESC NULLS LAST, pm.innings_number DESC");
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
