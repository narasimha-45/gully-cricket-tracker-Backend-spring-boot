package com.gullycricket.backend.matches.repository.read;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gullycricket.backend.config.DbQueryTimer;
import com.gullycricket.backend.matches.entity.MatchStatus;
import com.gullycricket.backend.matches.entity.MatchType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * High-traffic match summaries are read through JDBC so Hibernate never hydrates
 * the large match_data JSONB column or triggers one lazy innings query per match.
 *
 * <p>Test matches can have up to two innings per team, so per-team totals are exposed
 * both as an all-innings SUM (for aggregate stats) and as an ordered list of individual
 * innings (for display, e.g. "286 & 177-7"). See {@link MatchSummaryRow} for details.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MatchSummaryReadRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final DbQueryTimer queryTimer;
    private static final ObjectMapper INNINGS_JSON_MAPPER = new ObjectMapper();
    private static final TypeReference<List<InningsScoreRow>> INNINGS_LIST_TYPE = new TypeReference<>() {};

    private static final String BASE_SQL = """
            WITH innings_rows AS (
                SELECT
                    match_id,
                    batting_team_id,
                    team_innings_number,
                    runs,
                    wickets,
                    balls,
                    completed
                FROM match_innings_summary
                WHERE super_over = FALSE
            ),
            innings_totals AS (
                SELECT
                    match_id,
                    batting_team_id,
                    SUM(runs) AS runs,
                    SUM(wickets) AS wickets,
                    SUM(balls) AS balls
                FROM innings_rows
                GROUP BY match_id, batting_team_id
            ),
            innings_json AS (
                SELECT
                    match_id,
                    batting_team_id,
                    json_agg(
                        json_build_object(
                            'inningsNumber', team_innings_number,
                            'runs', runs,
                            'wickets', wickets,
                            'balls', balls,
                            'completed', completed
                        ) ORDER BY team_innings_number
                    ) AS innings
                FROM innings_rows
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
                ja.innings AS team_a_innings_json,
                tb.id AS team_b_id,
                tb.team_name AS team_b_name,
                COALESCE(ib.runs, m.team_b_score, 0) AS team_b_runs,
                COALESCE(ib.wickets, m.team_b_wickets, 0) AS team_b_wickets,
                COALESCE(ib.balls, m.team_b_balls_faced, 0) AS team_b_balls,
                jb.innings AS team_b_innings_json,
                m.batting_first_team_id AS batting_first_team_id,
                m.winner_team_id AS winner_team_id,
                winner.team_name AS winner_team_name,
                COALESCE(m.is_match_tied, FALSE) AS match_tied,
                COALESCE(m.is_match_drawn, FALSE) AS match_drawn,
                COALESCE(m.super_over, FALSE) AS super_over,
                m.won_by AS won_by,
                m.completed_at AS completed_at,
                m.status AS status,
                m.match_type AS match_type,
                m.total_overs AS total_overs
            FROM matches m
            JOIN seasons s ON s.id = m.season_id
            JOIN teams ta ON ta.id = m.team_a_id
            JOIN teams tb ON tb.id = m.team_b_id
            LEFT JOIN teams winner ON winner.id = m.winner_team_id
            LEFT JOIN innings_totals ia ON ia.match_id = m.id AND ia.batting_team_id = m.team_a_id
            LEFT JOIN innings_totals ib ON ib.match_id = m.id AND ib.batting_team_id = m.team_b_id
            LEFT JOIN innings_json ja ON ja.match_id = m.id AND ja.batting_team_id = m.team_a_id
            LEFT JOIN innings_json jb ON jb.match_id = m.id AND jb.batting_team_id = m.team_b_id
            WHERE 1 = 1
            """;

    public List<MatchSummaryRow> findBySeasonId(String seasonId) {
        MapSqlParameterSource params = new MapSqlParameterSource("seasonId", seasonId);
        return queryTimer.record("matches.bySeason", () -> jdbc.query(BASE_SQL + " AND m.season_id = :seasonId ORDER BY m.completed_at DESC NULLS LAST, m.id DESC", params, this::mapRow));
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
                parseInningsJson(rs.getString("team_a_innings_json")),
                rs.getString("team_b_id"),
                rs.getString("team_b_name"),
                rs.getInt("team_b_runs"),
                rs.getInt("team_b_wickets"),
                rs.getInt("team_b_balls"),
                parseInningsJson(rs.getString("team_b_innings_json")),
                rs.getString("batting_first_team_id"),
                rs.getString("winner_team_id"),
                rs.getString("winner_team_name"),
                rs.getBoolean("match_tied"),
                rs.getBoolean("match_drawn"),
                rs.getBoolean("super_over"),
                rs.getString("won_by"),
                rs.getTimestamp("completed_at") == null ? null : rs.getTimestamp("completed_at").toLocalDateTime(),
                MatchStatus.valueOf(rs.getString("status")),
                MatchType.valueOf(rs.getString("match_type")),
                getNullableInt(rs, "total_overs")
        );
    }

    private Integer getNullableInt(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    /** Postgres returns json_agg output as a JSON array string (or null if the team hasn't batted yet). */
    private List<InningsScoreRow> parseInningsJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return INNINGS_JSON_MAPPER.readValue(json, INNINGS_LIST_TYPE);
        } catch (Exception e) {
            log.warn("Failed to parse innings JSON for match summary row: {}", json, e);
            return List.of();
        }
    }
}
