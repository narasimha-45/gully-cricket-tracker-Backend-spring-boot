package com.gullycricket.backend.matches.repository.read;

import com.gullycricket.backend.config.DbQueryTimer;
import com.gullycricket.backend.matches.entity.MatchStatus;
import com.gullycricket.backend.matches.entity.MatchType;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * High-traffic match summaries are read through JDBC so Hibernate never hydrates
 * the large match_data JSONB column or triggers one lazy innings query per match.
 */
@Repository
@RequiredArgsConstructor
public class MatchSummaryReadRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final DbQueryTimer queryTimer;

    private static final String BASE_SQL = """
            WITH innings_totals AS (
                SELECT
                    match_id,
                    batting_team_id,
                    SUM(runs) AS runs,
                    SUM(wickets) AS wickets,
                    SUM(balls) AS balls
                FROM match_innings_summary
                WHERE super_over = FALSE
                GROUP BY match_id, batting_team_id
            )
            SELECT
                m.id AS match_id,
                s.id AS season_id,
                s.season_name AS season_name,
                ta.id AS team_a_id,
                ta.team_name AS team_a_name,
                COALESCE(ia.runs, m.team_a_score, 0) AS team_a_runs,
                COALESCE(ia.wickets, m.team_a_wickets, 0) AS team_a_wickets,
                COALESCE(ia.balls, m.team_a_balls_faced, 0) AS team_a_balls,
                tb.id AS team_b_id,
                tb.team_name AS team_b_name,
                COALESCE(ib.runs, m.team_b_score, 0) AS team_b_runs,
                COALESCE(ib.wickets, m.team_b_wickets, 0) AS team_b_wickets,
                COALESCE(ib.balls, m.team_b_balls_faced, 0) AS team_b_balls,
                m.batting_first_team_id AS batting_first_team_id,
                m.winner_team_id AS winner_team_id,
                winner.team_name AS winner_team_name,
                COALESCE(m.is_match_tied, FALSE) AS match_tied,
                COALESCE(m.is_match_drawn, FALSE) AS match_drawn,
                COALESCE(m.super_over, FALSE) AS super_over,
                m.won_by AS won_by,
                m.completed_at AS completed_at,
                m.status AS status,
                m.total_overs AS total_overs
            FROM matches m
            JOIN seasons s ON s.id = m.season_id
            JOIN teams ta ON ta.id = m.team_a_id
            JOIN teams tb ON tb.id = m.team_b_id
            LEFT JOIN teams winner ON winner.id = m.winner_team_id
            LEFT JOIN innings_totals ia ON ia.match_id = m.id AND ia.batting_team_id = m.team_a_id
            LEFT JOIN innings_totals ib ON ib.match_id = m.id AND ib.batting_team_id = m.team_b_id
            WHERE 1 = 1
            """;

    public List<MatchSummaryRow> findBySeasonId(String seasonId) {
        MapSqlParameterSource params = new MapSqlParameterSource("seasonId", seasonId);
        return queryTimer.record("matches.bySeason", () -> jdbc.query(BASE_SQL + " AND m.season_id = :seasonId ORDER BY m.completed_at DESC NULLS LAST, m.id DESC", params, this::mapRow));
    }

    private static final String INNINGS_BREAKDOWN_SQL = """
            SELECT
                mis.match_id AS match_id,
                mis.batting_team_id AS team_id,
                mis.team_innings_number AS team_innings_number,
                mis.runs AS runs,
                mis.wickets AS wickets,
                mis.balls AS balls,
                mis.completed AS completed,
                mis.is_follow_on AS is_follow_on,
                mis.completion_reason AS completion_reason
            FROM match_innings_summary mis
            JOIN matches m ON m.id = mis.match_id
            WHERE mis.super_over = FALSE AND m.season_id = :seasonId
            ORDER BY mis.match_id, mis.batting_team_id, mis.team_innings_number
            """;

    /**
     * Per-innings breakdown (runs/wickets/balls, declare/follow-on/completion reason)
     * for every completed match in a season, keyed by raw batting_team_id. Kept as a
     * separate query from {@link #findBySeasonId} since most callers only need the
     * flat score totals already carried on {@code matches}.
     */
    public List<MatchInningsBreakdownRow> findInningsBreakdownBySeasonId(String seasonId) {
        MapSqlParameterSource params = new MapSqlParameterSource("seasonId", seasonId);
        return queryTimer.record("matches.inningsBreakdownBySeason", () -> jdbc.query(INNINGS_BREAKDOWN_SQL, params, this::mapBreakdownRow));
    }

    private MatchInningsBreakdownRow mapBreakdownRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new MatchInningsBreakdownRow(
                rs.getString("match_id"),
                rs.getString("team_id"),
                rs.getInt("team_innings_number"),
                rs.getInt("runs"),
                rs.getInt("wickets"),
                rs.getInt("balls"),
                rs.getBoolean("completed"),
                rs.getBoolean("is_follow_on"),
                rs.getString("completion_reason")
        );
    }

    public List<MatchSummaryRow> findCompletedForTeam(String teamId, String seasonId) {
        return findCompletedForTeam(teamId, seasonId, null);
    }

    public List<MatchSummaryRow> findCompletedForTeam(String teamId, String seasonId, MatchType matchType) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("teamId", teamId)
                .addValue("completed", MatchStatus.COMPLETED.name());
        StringBuilder sql = new StringBuilder(BASE_SQL)
                .append(" AND m.status = :completed")
                .append(" AND (m.team_a_id = :teamId OR m.team_b_id = :teamId)");
        if (seasonId != null && !seasonId.isBlank()) {
            sql.append(" AND m.season_id = :seasonId");
            params.addValue("seasonId", seasonId.trim());
        }
        if (matchType != null) {
            sql.append(" AND m.match_type = :matchType");
            params.addValue("matchType", matchType.name());
        }
        sql.append(" ORDER BY m.completed_at DESC NULLS LAST, m.id DESC");
        return queryTimer.record("matches.completedForTeam", () -> jdbc.query(sql.toString(), params, this::mapRow));
    }

    public List<MatchSummaryRow> findCompleted(String seasonId) {
        return findCompleted(seasonId, null);
    }

    public List<MatchSummaryRow> findCompleted(String seasonId, MatchType matchType) {
        MapSqlParameterSource params = new MapSqlParameterSource("completed", MatchStatus.COMPLETED.name());
        StringBuilder sql = new StringBuilder(BASE_SQL).append(" AND m.status = :completed");
        if (seasonId != null && !seasonId.isBlank()) {
            sql.append(" AND m.season_id = :seasonId");
            params.addValue("seasonId", seasonId.trim());
        }
        if (matchType != null) {
            sql.append(" AND m.match_type = :matchType");
            params.addValue("matchType", matchType.name());
        }
        return queryTimer.record("matches.completed", () -> jdbc.query(sql.toString(), params, this::mapRow));
    }

    private MatchSummaryRow mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new MatchSummaryRow(
                rs.getString("match_id"),
                rs.getString("season_id"),
                rs.getString("season_name"),
                rs.getString("team_a_id"),
                rs.getString("team_a_name"),
                rs.getInt("team_a_runs"),
                rs.getInt("team_a_wickets"),
                rs.getInt("team_a_balls"),
                rs.getString("team_b_id"),
                rs.getString("team_b_name"),
                rs.getInt("team_b_runs"),
                rs.getInt("team_b_wickets"),
                rs.getInt("team_b_balls"),
                rs.getString("batting_first_team_id"),
                rs.getString("winner_team_id"),
                rs.getString("winner_team_name"),
                rs.getBoolean("match_tied"),
                rs.getBoolean("match_drawn"),
                rs.getBoolean("super_over"),
                rs.getString("won_by"),
                rs.getTimestamp("completed_at") == null ? null : rs.getTimestamp("completed_at").toLocalDateTime(),
                MatchStatus.valueOf(rs.getString("status")),
                getNullableInt(rs, "total_overs")
        );
    }

    private Integer getNullableInt(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }
}
