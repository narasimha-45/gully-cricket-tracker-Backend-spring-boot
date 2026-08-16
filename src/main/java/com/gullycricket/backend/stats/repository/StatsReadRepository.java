package com.gullycricket.backend.stats.repository;

import com.gullycricket.backend.config.DbQueryTimer;
import com.gullycricket.backend.matches.entity.MatchType;
import com.gullycricket.backend.stats.dto.BattingStatsFilter;
import com.gullycricket.backend.stats.dto.BattingStatsResponse;
import com.gullycricket.backend.stats.dto.BowlingStatsFilter;
import com.gullycricket.backend.stats.dto.BowlingStatsResponse;
import com.gullycricket.backend.stats.dto.FieldingAndMiscStatsFilter;
import com.gullycricket.backend.stats.dto.FieldingAndMiscStatsResponse;
import com.gullycricket.backend.stats.enums.BattingSortBy;
import com.gullycricket.backend.stats.enums.BestBowlingFigures;
import com.gullycricket.backend.stats.enums.BowlingSortBy;
import com.gullycricket.backend.stats.enums.FieldingSortBy;
import com.gullycricket.backend.stats.enums.MatchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Read-optimized statistics queries.
 *
 * <p>The write model intentionally remains JPA-based, but leaderboard reads do
 * not need fully hydrated PlayerMatch/Match aggregates. In particular, fetching
 * Match through JPA also reads the large match_data JSONB column. These queries
 * aggregate in PostgreSQL and return only the columns required by the API.</p>
 */
@Repository
@RequiredArgsConstructor
public class StatsReadRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final DbQueryTimer queryTimer;

    public List<BattingStatsResponse> findBattingLeaderboard(
            BattingStatsFilter filter,
            BattingSortBy sortBy,
            Integer minInnings,
            Integer limit
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = commonWhere(params, filter.seasonId(), filter.matchType(), filter.teamId(),
                filter.opponentTeamId(), filter.inningsNumber(), filter.result());
        if (filter.battingPosition() != null) {
            where += " AND pm.batting_position = :battingPosition";
            params.addValue("battingPosition", filter.battingPosition());
        }

        int safeMinInnings = minInnings == null ? 0 : Math.max(0, minInnings);
        int safeLimit = safeLimit(limit);
        params.addValue("minInnings", safeMinInnings);
        params.addValue("limit", safeLimit);

        String orderBy = switch (sortBy == null ? BattingSortBy.RUNS : sortBy) {
            case AVERAGE -> "batting_average DESC, total_runs DESC, player_name ASC";
            case STRIKE_RATE -> "strike_rate DESC, total_runs DESC, player_name ASC";
            case HIGHEST_SCORE -> "highest_score DESC, total_runs DESC, player_name ASC";
            case FOURS -> "total_fours DESC, total_runs DESC, player_name ASC";
            case SIXES -> "total_sixes DESC, total_runs DESC, player_name ASC";
            case MATCHES -> "matches_played DESC, total_runs DESC, player_name ASC";
            case RUNS -> "total_runs DESC, strike_rate DESC, player_name ASC";
        };

        String sql = """
                SELECT
                    pm.player_id AS player_id,
                    p.name AS player_name,
                    COALESCE(SUM(pm.runs_scored), 0) AS total_runs,
                    COALESCE(SUM(pm.balls_faced), 0) AS total_balls,
                    COALESCE(SUM(pm.fours_hit), 0) AS total_fours,
                    COALESCE(SUM(pm.sixes_hit), 0) AS total_sixes,
                    COALESCE(SUM(CASE WHEN pm.out = FALSE THEN 1 ELSE 0 END), 0) AS not_outs,
                    COALESCE(MAX(pm.runs_scored), 0) AS highest_score,
                    COALESCE(SUM(CASE WHEN pm.out = TRUE AND pm.runs_scored = 0 THEN 1 ELSE 0 END), 0) AS ducks,
                    COUNT(DISTINCT pm.match_id) AS matches_played,
                    COUNT(*) AS innings_played,
                    COALESCE(SUM(pm.dot_balls_played), 0) AS dot_balls,
                    CASE
                        WHEN COALESCE(SUM(pm.balls_faced), 0) = 0 THEN 0
                        ELSE SUM(pm.runs_scored)::double precision * 100.0 / SUM(pm.balls_faced)
                    END AS strike_rate,
                    CASE
                        WHEN SUM(CASE WHEN pm.out = TRUE THEN 1 ELSE 0 END) = 0
                            THEN COALESCE(SUM(pm.runs_scored), 0)::double precision
                        ELSE SUM(pm.runs_scored)::double precision /
                             SUM(CASE WHEN pm.out = TRUE THEN 1 ELSE 0 END)
                    END AS batting_average
                FROM player_matches pm
                JOIN players p ON p.id = pm.player_id
                JOIN matches m ON m.id = pm.match_id
                WHERE pm.batted = TRUE
                """ + where + """
                GROUP BY pm.player_id, p.name
                HAVING COUNT(*) >= :minInnings
                ORDER BY """ + orderBy + " LIMIT :limit";

        return queryTimer.record("stats.battingLeaderboard", () -> jdbc.query(sql, params, (rs, rowNum) -> new BattingStatsResponse(
                rs.getString("player_id"),
                rs.getString("player_name"),
                rs.getInt("total_runs"),
                rs.getInt("total_balls"),
                round2(rs.getDouble("strike_rate")),
                rs.getInt("total_fours"),
                rs.getInt("total_sixes"),
                rs.getInt("not_outs"),
                round2(rs.getDouble("batting_average")),
                rs.getInt("highest_score"),
                rs.getInt("ducks"),
                rs.getInt("matches_played"),
                rs.getInt("innings_played"),
                rs.getInt("dot_balls")
        )));
    }

    public List<BowlingStatsResponse> findBowlingLeaderboard(
            BowlingStatsFilter filter,
            BowlingSortBy sortBy,
            Integer minInnings,
            Integer limit
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = commonWhere(params, filter.seasonId(), filter.matchType(), filter.teamId(),
                filter.opponentTeamId(), filter.inningsNumber(), filter.result());

        int safeMinInnings = minInnings == null ? 0 : Math.max(0, minInnings);
        int safeLimit = safeLimit(limit);
        params.addValue("minInnings", safeMinInnings);
        params.addValue("limit", safeLimit);

        String orderBy = switch (sortBy == null ? BowlingSortBy.WICKETS : sortBy) {
            case ECONOMY -> "economy_rate ASC NULLS LAST, total_wickets DESC, player_name ASC";
            case AVERAGE -> "bowling_average ASC NULLS LAST, total_wickets DESC, player_name ASC";
            case MATCHES -> "matches_played DESC, total_wickets DESC, player_name ASC";
            case WICKETS -> "total_wickets DESC, bowling_average ASC NULLS LAST, player_name ASC";
        };

        String sql = """
                WITH filtered AS (
                    SELECT
                        pm.id,
                        pm.player_id,
                        p.name AS player_name,
                        pm.match_id,
                        pm.wickets_taken,
                        pm.runs_conceded,
                        pm.balls_bowled,
                        pm.maidens_bowled,
                        pm.dot_balls_bowled
                    FROM player_matches pm
                    JOIN players p ON p.id = pm.player_id
                    JOIN matches m ON m.id = pm.match_id
                    WHERE pm.bowled = TRUE
                """ + where + """
                ),
                aggregate_stats AS (
                    SELECT
                        player_id,
                        MAX(player_name) AS player_name,
                        COALESCE(SUM(wickets_taken), 0) AS total_wickets,
                        COALESCE(SUM(runs_conceded), 0) AS total_runs_conceded,
                        COALESCE(SUM(balls_bowled), 0) AS total_balls_bowled,
                        COALESCE(SUM(maidens_bowled), 0) AS total_maidens,
                        COALESCE(SUM(dot_balls_bowled), 0) AS dot_balls_bowled,
                        COUNT(DISTINCT match_id) AS matches_played,
                        COUNT(*) AS innings_bowled,
                        SUM(CASE WHEN wickets_taken >= 5 THEN 1 ELSE 0 END) AS five_wicket_hauls,
                        CASE
                            WHEN COALESCE(SUM(balls_bowled), 0) = 0 THEN NULL
                            ELSE SUM(runs_conceded)::double precision * 6.0 / SUM(balls_bowled)
                        END AS economy_rate,
                        CASE
                            WHEN COALESCE(SUM(wickets_taken), 0) = 0 THEN NULL
                            ELSE SUM(runs_conceded)::double precision / SUM(wickets_taken)
                        END AS bowling_average
                    FROM filtered
                    GROUP BY player_id
                ),
                ranked_figures AS (
                    SELECT
                        player_id,
                        wickets_taken,
                        runs_conceded,
                        balls_bowled,
                        ROW_NUMBER() OVER (
                            PARTITION BY player_id
                            ORDER BY wickets_taken DESC, runs_conceded ASC, balls_bowled ASC, id ASC
                        ) AS rn
                    FROM filtered
                ),
                ten_haul AS (
                    SELECT player_id, COUNT(*) AS ten_wicket_hauls
                    FROM (
                        SELECT player_id, match_id, SUM(wickets_taken) AS match_wickets
                        FROM filtered
                        GROUP BY player_id, match_id
                        HAVING SUM(wickets_taken) >= 10
                    ) mw
                    GROUP BY player_id
                )
                SELECT
                    a.*,
                    r.wickets_taken AS best_wickets,
                    r.runs_conceded AS best_runs_conceded,
                    r.balls_bowled AS best_balls_bowled,
                    COALESCE(t.ten_wicket_hauls, 0) AS ten_wicket_hauls
                FROM aggregate_stats a
                JOIN ranked_figures r ON r.player_id = a.player_id AND r.rn = 1
                LEFT JOIN ten_haul t ON t.player_id = a.player_id
                WHERE a.innings_bowled >= :minInnings
                ORDER BY """ + orderBy + " LIMIT :limit";

        return queryTimer.record("stats.bowlingLeaderboard", () -> jdbc.query(sql, params, (rs, rowNum) -> {
            int totalBalls = rs.getInt("total_balls_bowled");
            Double average = nullableDouble(rs, "bowling_average");
            Double economy = nullableDouble(rs, "economy_rate");
            return new BowlingStatsResponse(
                    rs.getString("player_id"),
                    rs.getString("player_name"),
                    rs.getInt("total_wickets"),
                    rs.getInt("total_runs_conceded"),
                    economy == null ? 0.0 : round2(economy),
                    toCricketOvers(totalBalls),
                    rs.getInt("total_maidens"),
                    average == null ? null : round2(average),
                    new BestBowlingFigures(
                            rs.getInt("best_wickets"),
                            rs.getInt("best_runs_conceded"),
                            rs.getInt("best_balls_bowled")
                    ),
                    rs.getInt("five_wicket_hauls"),
                    rs.getInt("ten_wicket_hauls"),
                    rs.getInt("matches_played"),
                    rs.getInt("innings_bowled"),
                    rs.getInt("dot_balls_bowled")
            );
        }));
    }

    public List<FieldingAndMiscStatsResponse> findFieldingLeaderboard(
            FieldingAndMiscStatsFilter filter,
            FieldingSortBy sortBy,
            Integer limit
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = commonWhere(params, filter.seasonId(), filter.matchType(), filter.teamId(),
                filter.opponentTeamId(), filter.inningsNumber(), filter.result());
        params.addValue("limit", safeLimit(limit));

        String orderBy = switch (sortBy == null ? FieldingSortBy.DISMISSALS : sortBy) {
            case CATCHES -> "total_catches DESC, player_name ASC";
            case RUN_OUTS -> "total_run_outs DESC, player_name ASC";
            case STUMPINGS -> "total_stumpings DESC, player_name ASC";
            case MAN_OF_THE_MATCH -> "motm_awards DESC, player_name ASC";
            case DISMISSALS -> "(SUM(pm.catches_taken) + SUM(pm.run_outs) + SUM(pm.stumpings)) DESC, p.name ASC";
        };

        String sql = """
                SELECT
                    pm.player_id AS player_id,
                    p.name AS player_name,
                    COALESCE(SUM(pm.catches_taken), 0) AS total_catches,
                    COALESCE(SUM(pm.run_outs), 0) AS total_run_outs,
                    COALESCE(SUM(pm.stumpings), 0) AS total_stumpings,
                    COUNT(DISTINCT pm.match_id) AS matches_played,
                    COUNT(DISTINCT CASE WHEN pm.player_of_the_match = TRUE THEN pm.match_id END) AS motm_awards
                FROM player_matches pm
                JOIN players p ON p.id = pm.player_id
                JOIN matches m ON m.id = pm.match_id
                WHERE 1 = 1
                """ + where + """
                GROUP BY pm.player_id, p.name
                ORDER BY """ + orderBy + " LIMIT :limit";

        return queryTimer.record("stats.fieldingLeaderboard", () -> jdbc.query(sql, params, (rs, rowNum) -> new FieldingAndMiscStatsResponse(
                rs.getString("player_id"),
                rs.getString("player_name"),
                rs.getInt("total_catches"),
                rs.getInt("total_run_outs"),
                rs.getInt("total_stumpings"),
                rs.getInt("matches_played"),
                rs.getInt("motm_awards")
        )));
    }

    private String commonWhere(
            MapSqlParameterSource params,
            String seasonId,
            MatchType matchType,
            String teamId,
            String opponentTeamId,
            Integer inningsNumber,
            MatchResult result
    ) {
        StringBuilder where = new StringBuilder();
        if (hasText(seasonId)) {
            where.append(" AND pm.season_id = :seasonId");
            params.addValue("seasonId", seasonId.trim());
        }
        if (matchType != null) {
            where.append(" AND pm.match_type = :matchType");
            params.addValue("matchType", matchType.name());
        }
        if (hasText(teamId)) {
            where.append(" AND pm.team_represented_id = :teamId");
            params.addValue("teamId", teamId.trim());
        }
        if (hasText(opponentTeamId)) {
            where.append(" AND pm.opposition_team_id = :opponentTeamId");
            params.addValue("opponentTeamId", opponentTeamId.trim());
        }
        if (inningsNumber != null) {
            where.append(" AND pm.innings_number = :inningsNumber");
            params.addValue("inningsNumber", inningsNumber);
        }
        if (result != null) {
            where.append(switch (result) {
                case WIN -> " AND pm.match_won = TRUE AND COALESCE(m.is_match_tied, FALSE) = FALSE AND COALESCE(m.is_match_drawn, FALSE) = FALSE";
                case LOSS -> " AND pm.match_won = FALSE AND COALESCE(m.is_match_tied, FALSE) = FALSE AND COALESCE(m.is_match_drawn, FALSE) = FALSE AND m.winner_team_id IS NOT NULL";
                case TIE -> " AND COALESCE(m.is_match_tied, FALSE) = TRUE";
                case NO_RESULT -> " AND (COALESCE(m.is_match_drawn, FALSE) = TRUE OR (m.winner_team_id IS NULL AND COALESCE(m.is_match_tied, FALSE) = FALSE))";
            });
        }
        return where.toString();
    }

    private int safeLimit(Integer limit) {
        return limit == null ? 50 : Math.max(1, Math.min(limit, 100));
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static double toCricketOvers(int balls) {
        return (balls / 6) + ((balls % 6) / 10.0);
    }

    private static Double nullableDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
