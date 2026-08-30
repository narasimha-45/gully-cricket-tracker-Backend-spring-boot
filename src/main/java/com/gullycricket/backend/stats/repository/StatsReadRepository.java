package com.gullycricket.backend.stats.repository;

import com.gullycricket.backend.config.DbQueryTimer;
import com.gullycricket.backend.matches.entity.MatchType;
import com.gullycricket.backend.stats.dto.BattingStatsFilter;
import com.gullycricket.backend.stats.dto.BattingStatsResponse;
import com.gullycricket.backend.stats.dto.BowlingStatsFilter;
import com.gullycricket.backend.stats.dto.BowlingStatsResponse;
import com.gullycricket.backend.stats.dto.FieldingAndMiscStatsFilter;
import com.gullycricket.backend.stats.dto.FieldingAndMiscStatsResponse;
import com.gullycricket.backend.stats.dto.PartnershipInningsDto;
import com.gullycricket.backend.stats.dto.PartnershipStatsFilter;
import com.gullycricket.backend.stats.dto.PartnershipStatsResponse;
import com.gullycricket.backend.stats.dto.PlayerParticipationSummaryDto;
import com.gullycricket.backend.stats.dto.RivalryInningsDto;
import com.gullycricket.backend.stats.dto.RivalryStatsFilter;
import com.gullycricket.backend.stats.dto.RivalryStatsResponse;
import com.gullycricket.backend.stats.enums.BattingSortBy;
import com.gullycricket.backend.stats.enums.BestBowlingFigures;
import com.gullycricket.backend.stats.enums.BowlingSortBy;
import com.gullycricket.backend.stats.enums.FieldingSortBy;
import com.gullycricket.backend.stats.enums.MatchResult;
import com.gullycricket.backend.players.entity.DismissalType;
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
public class StatsReadRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final DbQueryTimer queryTimer;

    public StatsReadRepository(NamedParameterJdbcTemplate jdbc, DbQueryTimer queryTimer) {
        this.jdbc = jdbc;
        this.queryTimer = queryTimer;
    }

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
            case AVERAGE -> "batting_average DESC NULLS LAST, total_runs DESC, player_name ASC";
            case STRIKE_RATE -> "strike_rate DESC, total_runs DESC, player_name ASC";
            case HIGHEST_SCORE -> "highest_score DESC, total_runs DESC, player_name ASC";
            case FOURS -> "total_fours DESC, total_runs DESC, player_name ASC";
            case SIXES -> "total_sixes DESC, total_runs DESC, player_name ASC";
            case MATCHES -> "matches_played DESC, total_runs DESC, player_name ASC";
            case RUNS -> "total_runs DESC, strike_rate DESC, player_name ASC";
        };

        String sql = buildBattingSql(where, orderBy, participationCountExpression(filter.seasonId(), filter.matchType(), filter.teamId(), filter.opponentTeamId(), filter.result(), "pm.player_id"));

        return queryTimer.record("stats.battingLeaderboard", () -> jdbc.query(sql, params, (rs, rowNum) -> {
            Double average = nullableDouble(rs, "batting_average");
            return new BattingStatsResponse(
                    rs.getString("player_id"), rs.getString("player_name"), rs.getInt("total_runs"),
                    rs.getInt("total_balls"), round2(rs.getDouble("strike_rate")), rs.getInt("total_fours"),
                    rs.getInt("total_sixes"), rs.getInt("not_outs"), average == null ? null : round2(average),
                    rs.getInt("highest_score"), rs.getInt("ducks"), rs.getInt("matches_played"),
                    rs.getInt("innings_played"), rs.getInt("dot_balls")
            );
        }));
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

        String sql = buildBowlingSql(where, orderBy, participationCountExpression(filter.seasonId(), filter.matchType(), filter.teamId(), filter.opponentTeamId(), filter.result(), "f.player_id"));

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

        String sql = buildFieldingSql(where, orderBy, participationCountExpression(filter.seasonId(), filter.matchType(), filter.teamId(), filter.opponentTeamId(), filter.result(), "pm.player_id"));

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


    public PlayerParticipationSummaryDto findPlayerParticipationSummary(String playerId, String seasonId) {
        return findPlayerParticipationSummary(playerId, seasonId, null);
    }

    public PlayerParticipationSummaryDto findPlayerParticipationSummary(String playerId, String seasonId, MatchType matchType) {
        MapSqlParameterSource params = new MapSqlParameterSource("playerId", playerId);
        StringBuilder extraClause = new StringBuilder();
        if (hasText(seasonId)) {
            extraClause.append(" AND season_id = :seasonId");
            params.addValue("seasonId", seasonId.trim());
        }
        if (matchType != null) {
            extraClause.append(" AND match_type = :matchType");
            params.addValue("matchType", matchType.name());
        }
        String sql = """
                SELECT COUNT(DISTINCT match_id) AS matches_played,
                       COUNT(DISTINCT CASE WHEN match_won = TRUE THEN match_id END) AS matches_won,
                       COUNT(DISTINCT CASE WHEN player_of_the_match = TRUE THEN match_id END) AS motm
                FROM match_player_participation
                WHERE player_id = :playerId
                %s
                """.formatted(extraClause);
        return queryTimer.record("stats.playerParticipation", () -> jdbc.queryForObject(sql, params, (rs, rowNum) ->
                new PlayerParticipationSummaryDto(rs.getInt("matches_played"), rs.getInt("matches_won"), rs.getInt("motm"))));
    }

    /**
     * Returns individual partnership occurrences (one row per partnership in one innings).
     * Ordered by runs descending so this endpoint can directly power the "highest partnerships
     * in an innings" leaderboard.
     */
    public List<PartnershipInningsDto> findPartnershipInnings(PartnershipStatsFilter filter, Integer limit) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = partnershipWhere(params, filter);
        params.addValue("limit", safeLimit(limit));

        String sql = """
                SELECT pp.id, pp.match_id, pp.season_id, s.season_name,
                       pp.team_represented_id, team.team_name,
                       opponent.id AS opponent_team_id, opponent.team_name AS opponent_team_name,
                       pp.player1_id, p1.name AS player1_name, pp.player2_id, p2.name AS player2_name,
                       pp.innings_number, pp.partnership_number, pp.runs_scored, pp.balls_faced,
                       pp.player1_runs, pp.player1_balls_faced, pp.player2_runs, pp.player2_balls_faced,
                       pp.fours_hit, pp.sixes_hit,
                       pp.player1_fours_hit, pp.player1_sixes_hit, pp.player2_fours_hit, pp.player2_sixes_hit,
                       pp.partnership_broken, pp.who_got_out_id, pp.match_won, m.completed_at
                FROM player_partnerships pp
                JOIN players p1 ON p1.id = pp.player1_id
                JOIN players p2 ON p2.id = pp.player2_id
                JOIN matches m ON m.id = pp.match_id
                JOIN seasons s ON s.id = pp.season_id
                JOIN teams team ON team.id = pp.team_represented_id
                JOIN teams opponent ON opponent.id = CASE
                    WHEN m.team_a_id = pp.team_represented_id THEN m.team_b_id
                    ELSE m.team_a_id
                END
                %s
                ORDER BY pp.runs_scored DESC, pp.balls_faced ASC,
                         m.completed_at DESC NULLS LAST, pp.id ASC
                LIMIT :limit
                """.formatted(where);

        return queryTimer.record("stats.partnershipInnings", () -> jdbc.query(sql, params, this::mapPartnershipInnings));
    }

    /**
     * Aggregates all partnership occurrences for the same player pair. The pair is normalized by
     * player id so legacy rows stored in opposite player1/player2 order still collapse into one row.
     */
    public List<PartnershipStatsResponse> findPartnershipAggregatedStats(PartnershipStatsFilter filter, Integer limit) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = partnershipWhere(params, filter);
        params.addValue("limit", safeLimit(limit));

        String sql = """
                WITH filtered AS (
                    SELECT
                        pp.*,
                        CASE WHEN pp.player1_id <= pp.player2_id THEN pp.player1_id ELSE pp.player2_id END AS pair_player1_id,
                        CASE WHEN pp.player1_id <= pp.player2_id THEN pp.player2_id ELSE pp.player1_id END AS pair_player2_id
                    FROM player_partnerships pp
                    JOIN matches m ON m.id = pp.match_id
                    %s
                ), normalized AS (
                    SELECT
                        f.*,
                        CASE WHEN f.player1_id = f.pair_player1_id THEN f.player1_runs ELSE f.player2_runs END AS pair_player1_runs,
                        CASE WHEN f.player1_id = f.pair_player1_id THEN f.player2_runs ELSE f.player1_runs END AS pair_player2_runs,
                        CASE WHEN f.player1_id = f.pair_player1_id THEN f.player1_balls_faced ELSE f.player2_balls_faced END AS pair_player1_balls,
                        CASE WHEN f.player1_id = f.pair_player1_id THEN f.player2_balls_faced ELSE f.player1_balls_faced END AS pair_player2_balls,
                        CASE WHEN f.player1_id = f.pair_player1_id THEN f.player1_fours_hit ELSE f.player2_fours_hit END AS pair_player1_fours,
                        CASE WHEN f.player1_id = f.pair_player1_id THEN f.player2_fours_hit ELSE f.player1_fours_hit END AS pair_player2_fours,
                        CASE WHEN f.player1_id = f.pair_player1_id THEN f.player1_sixes_hit ELSE f.player2_sixes_hit END AS pair_player1_sixes,
                        CASE WHEN f.player1_id = f.pair_player1_id THEN f.player2_sixes_hit ELSE f.player1_sixes_hit END AS pair_player2_sixes,
                        CASE WHEN f.player1_id = f.pair_player1_id THEN f.player1_dot_balls ELSE f.player2_dot_balls END AS pair_player1_dots,
                        CASE WHEN f.player1_id = f.pair_player1_id THEN f.player2_dot_balls ELSE f.player1_dot_balls END AS pair_player2_dots
                    FROM filtered f
                )
                SELECT
                    n.pair_player1_id AS player1_id, p1.name AS player1_name,
                    n.pair_player2_id AS player2_id, p2.name AS player2_name,
                    COUNT(DISTINCT n.match_id) AS total_matches,
                    COUNT(DISTINCT (n.match_id || ':' || n.innings_number::text)) AS total_innings,
                    COUNT(*) AS total_partnerships,
                    COALESCE(SUM(n.runs_scored), 0) AS total_runs,
                    COALESCE(SUM(n.balls_faced), 0) AS total_balls,
                    COALESCE(SUM(n.dot_balls), 0) AS total_dot_balls,
                    COALESCE(SUM(n.fours_hit), 0) AS total_fours,
                    COALESCE(SUM(n.sixes_hit), 0) AS total_sixes,
                    CASE WHEN SUM(n.balls_faced) = 0 THEN 0
                         ELSE SUM(n.runs_scored)::double precision * 6.0 / SUM(n.balls_faced) END AS run_rate,
                    SUM(n.runs_scored)::double precision / NULLIF(COUNT(*), 0) AS partnership_average,
                    CASE WHEN SUM(n.balls_faced) = 0 THEN 0
                         ELSE SUM(n.dot_balls)::double precision * 100.0 / SUM(n.balls_faced) END AS dot_percentage,
                    COALESCE(MAX(n.runs_scored), 0) AS highest_partnership,
                    COALESCE(MIN(n.runs_scored), 0) AS lowest_partnership,
                    SUM(CASE WHEN n.partnership_broken = FALSE THEN 1 ELSE 0 END) AS unbeaten_partnerships,
                    SUM(CASE WHEN n.runs_scored >= 50 AND n.runs_scored < 100 THEN 1 ELSE 0 END) AS fifty_plus,
                    SUM(CASE WHEN n.runs_scored >= 100 THEN 1 ELSE 0 END) AS hundred_plus,
                    COALESCE(SUM(n.pair_player1_runs), 0) AS player1_runs,
                    COALESCE(SUM(n.pair_player2_runs), 0) AS player2_runs,
                    COALESCE(SUM(n.pair_player1_balls), 0) AS player1_balls,
                    COALESCE(SUM(n.pair_player2_balls), 0) AS player2_balls,
                    COALESCE(SUM(n.pair_player1_fours), 0) AS player1_fours,
                    COALESCE(SUM(n.pair_player2_fours), 0) AS player2_fours,
                    COALESCE(SUM(n.pair_player1_sixes), 0) AS player1_sixes,
                    COALESCE(SUM(n.pair_player2_sixes), 0) AS player2_sixes,
                    COALESCE(SUM(n.pair_player1_dots), 0) AS player1_dots,
                    COALESCE(SUM(n.pair_player2_dots), 0) AS player2_dots
                FROM normalized n
                JOIN players p1 ON p1.id = n.pair_player1_id
                JOIN players p2 ON p2.id = n.pair_player2_id
                GROUP BY n.pair_player1_id, p1.name, n.pair_player2_id, p2.name
                ORDER BY total_runs DESC, highest_partnership DESC, total_partnerships DESC,
                         player1_name ASC, player2_name ASC
                LIMIT :limit
                """.formatted(where);

        return queryTimer.record("stats.partnershipsAggregated", () -> jdbc.query(sql, params, (rs, rowNum) -> new PartnershipStatsResponse(
                rs.getString("player1_id"), rs.getString("player1_name"), rs.getString("player2_id"), rs.getString("player2_name"),
                rs.getInt("total_matches"), rs.getInt("total_innings"), rs.getInt("total_partnerships"),
                rs.getInt("total_runs"), rs.getInt("total_balls"), rs.getInt("total_dot_balls"), rs.getInt("total_fours"), rs.getInt("total_sixes"),
                round2(rs.getDouble("run_rate")), round2(rs.getDouble("partnership_average")), round2(rs.getDouble("dot_percentage")),
                rs.getInt("highest_partnership"), rs.getInt("lowest_partnership"), rs.getInt("unbeaten_partnerships"),
                rs.getInt("fifty_plus"), rs.getInt("hundred_plus"),
                rs.getInt("player1_runs"), rs.getInt("player2_runs"), rs.getInt("player1_balls"), rs.getInt("player2_balls"),
                rs.getInt("player1_fours"), rs.getInt("player2_fours"), rs.getInt("player1_sixes"), rs.getInt("player2_sixes"),
                rs.getInt("player1_dots"), rs.getInt("player2_dots")
        )));
    }

    /** Backward-compatible repository alias for callers that previously treated partnerships as aggregated stats. */
    public List<PartnershipStatsResponse> findPartnershipStats(PartnershipStatsFilter filter, Integer limit) {
        return findPartnershipAggregatedStats(filter, limit);
    }

    public List<PartnershipInningsDto> findPartnershipHistory(String playerId, String partnerId, String seasonId, Integer limit) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("playerId", playerId)
                .addValue("partnerId", partnerId)
                .addValue("limit", safeLimit(limit));
        String seasonClause = "";
        if (hasText(seasonId)) {
            seasonClause = " AND pp.season_id = :seasonId";
            params.addValue("seasonId", seasonId.trim());
        }
        String sql = """
                SELECT pp.id, pp.match_id, pp.season_id, s.season_name,
                       pp.team_represented_id, team.team_name,
                       opponent.id AS opponent_team_id, opponent.team_name AS opponent_team_name,
                       pp.player1_id, p1.name AS player1_name, pp.player2_id, p2.name AS player2_name,
                       pp.innings_number, pp.partnership_number, pp.runs_scored, pp.balls_faced,
                       pp.player1_runs, pp.player1_balls_faced, pp.player2_runs, pp.player2_balls_faced,
                       pp.fours_hit, pp.sixes_hit,
                       pp.player1_fours_hit, pp.player1_sixes_hit, pp.player2_fours_hit, pp.player2_sixes_hit,
                       pp.partnership_broken, pp.who_got_out_id, pp.match_won, m.completed_at
                FROM player_partnerships pp
                JOIN players p1 ON p1.id = pp.player1_id
                JOIN players p2 ON p2.id = pp.player2_id
                JOIN matches m ON m.id = pp.match_id
                JOIN seasons s ON s.id = pp.season_id
                JOIN teams team ON team.id = pp.team_represented_id
                JOIN teams opponent ON opponent.id = CASE WHEN m.team_a_id = pp.team_represented_id THEN m.team_b_id ELSE m.team_a_id END
                WHERE ((pp.player1_id = :playerId AND pp.player2_id = :partnerId)
                    OR (pp.player1_id = :partnerId AND pp.player2_id = :playerId))
                %s
                ORDER BY m.completed_at DESC NULLS LAST, pp.innings_number DESC, pp.partnership_number DESC
                LIMIT :limit
                """.formatted(seasonClause);
        return queryTimer.record("stats.partnershipHistory", () -> jdbc.query(sql, params, this::mapPartnershipInnings));
    }

    public List<RivalryStatsResponse> findRivalryStats(RivalryStatsFilter filter, Integer limit) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = rivalryWhere(params, filter);
        int minBalls = filter.minBallsFaced() == null ? 0 : Math.max(0, filter.minBallsFaced());
        int minRuns = filter.minRuns() == null ? 0 : Math.max(0, filter.minRuns());
        int minDismissals = filter.minDismissals() == null ? 0 : Math.max(0, filter.minDismissals());
        params.addValue("minBalls", minBalls).addValue("minRuns", minRuns)
                .addValue("minDismissals", minDismissals).addValue("limit", safeLimit(limit));

        String wicketExpression = "SUM(CASE WHEN pr.batsman_dismissed = TRUE AND pr.dismissal_type IN ('BOWLED','CAUGHT','LBW','STUMPED','HIT_WICKET','SPECIAL_CASE') THEN 1 ELSE 0 END)";
        String sql = """
                SELECT pr.batsman_id AS batter_id, batter.name AS batter_name,
                       pr.bowler_id, bowler.name AS bowler_name,
                       COUNT(*) AS total_innings,
                       COALESCE(SUM(pr.runs_scored), 0) AS total_runs,
                       COALESCE(SUM(pr.balls_faced), 0) AS total_balls,
                       COALESCE(SUM(pr.dot_balls), 0) AS total_dots,
                       COALESCE(SUM(pr.fours_hit), 0) AS total_fours,
                       COALESCE(SUM(pr.sixes_hit), 0) AS total_sixes,
                       CASE WHEN SUM(pr.balls_faced) = 0 THEN 0 ELSE SUM(pr.runs_scored)::double precision * 100.0 / SUM(pr.balls_faced) END AS strike_rate,
                       CASE WHEN %1$s = 0 THEN NULL ELSE SUM(pr.runs_scored)::double precision / %1$s END AS average_runs,
                       CASE WHEN SUM(pr.balls_faced) = 0 THEN 0 ELSE SUM(pr.dot_balls)::double precision * 100.0 / SUM(pr.balls_faced) END AS dot_percentage,
                       %1$s AS wickets_taken
                FROM player_rivalries pr
                JOIN players batter ON batter.id = pr.batsman_id
                JOIN players bowler ON bowler.id = pr.bowler_id
                JOIN matches m ON m.id = pr.match_id
                JOIN player_matches bpm ON bpm.match_id = pr.match_id AND bpm.player_id = pr.batsman_id AND bpm.innings_number = pr.innings_number
                %2$s
                GROUP BY pr.batsman_id, batter.name, pr.bowler_id, bowler.name
                HAVING SUM(pr.balls_faced) >= :minBalls AND SUM(pr.runs_scored) >= :minRuns AND %1$s >= :minDismissals
                ORDER BY wickets_taken DESC, total_runs DESC, total_balls DESC, batter_name ASC, bowler_name ASC
                LIMIT :limit
                """.formatted(wicketExpression, where);

        return queryTimer.record("stats.rivalries", () -> jdbc.query(sql, params, (rs, rowNum) -> {
            Double average = nullableDouble(rs, "average_runs");
            return new RivalryStatsResponse(
                    rs.getString("batter_id"), rs.getString("batter_name"), rs.getString("bowler_id"), rs.getString("bowler_name"),
                    rs.getInt("total_innings"), rs.getInt("total_runs"), rs.getInt("total_balls"), rs.getInt("total_dots"),
                    rs.getInt("total_fours"), rs.getInt("total_sixes"), round2(rs.getDouble("strike_rate")),
                    average == null ? null : round2(average), round2(rs.getDouble("dot_percentage")), rs.getInt("wickets_taken")
            );
        }));
    }

    /** Returns one batter-vs-bowler row per innings, useful for detailed player-vs-player history. */
    public List<RivalryInningsDto> findRivalryInnings(RivalryStatsFilter filter, Integer limit) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = rivalryWhere(params, filter);
        params.addValue("limit", safeLimit(limit));

        String sql = """
                SELECT pr.id, pr.match_id, pr.season_id, s.season_name,
                       bpm.team_represented_id AS batter_team_id, batter_team.team_name AS batter_team_name,
                       bpm.opposition_team_id AS opponent_team_id, opponent.team_name AS opponent_team_name,
                       pr.batsman_id AS batter_id, batter.name AS batter_name,
                       pr.bowler_id, bowler.name AS bowler_name,
                       pr.innings_number, pr.runs_scored, pr.balls_faced, pr.dot_balls,
                       pr.fours_hit, pr.sixes_hit,
                       CASE WHEN pr.balls_faced = 0 THEN 0 ELSE pr.runs_scored::double precision * 100.0 / pr.balls_faced END AS strike_rate,
                       pr.batsman_dismissed, pr.dismissal_type, bpm.match_won, m.completed_at
                FROM player_rivalries pr
                JOIN players batter ON batter.id = pr.batsman_id
                JOIN players bowler ON bowler.id = pr.bowler_id
                JOIN matches m ON m.id = pr.match_id
                JOIN seasons s ON s.id = pr.season_id
                JOIN player_matches bpm ON bpm.match_id = pr.match_id AND bpm.player_id = pr.batsman_id AND bpm.innings_number = pr.innings_number
                JOIN teams batter_team ON batter_team.id = bpm.team_represented_id
                JOIN teams opponent ON opponent.id = bpm.opposition_team_id
                %s
                ORDER BY m.completed_at DESC NULLS LAST, pr.innings_number DESC, pr.id ASC
                LIMIT :limit
                """.formatted(where);

        return queryTimer.record("stats.rivalryInnings", () -> jdbc.query(sql, params, (rs, rowNum) -> new RivalryInningsDto(
                rs.getString("id"), rs.getString("match_id"), rs.getString("season_id"), rs.getString("season_name"),
                rs.getString("batter_team_id"), rs.getString("batter_team_name"), rs.getString("opponent_team_id"), rs.getString("opponent_team_name"),
                rs.getString("batter_id"), rs.getString("batter_name"), rs.getString("bowler_id"), rs.getString("bowler_name"),
                rs.getInt("innings_number"), rs.getInt("runs_scored"), rs.getInt("balls_faced"), rs.getInt("dot_balls"),
                rs.getInt("fours_hit"), rs.getInt("sixes_hit"), round2(rs.getDouble("strike_rate")),
                rs.getBoolean("batsman_dismissed"), dismissalType(rs.getString("dismissal_type")), rs.getBoolean("match_won"),
                rs.getTimestamp("completed_at") == null ? null : rs.getTimestamp("completed_at").toLocalDateTime()
        )));
    }

    private PartnershipInningsDto mapPartnershipInnings(ResultSet rs, int rowNum) throws SQLException {
        return new PartnershipInningsDto(
                rs.getString("id"), rs.getString("match_id"), rs.getString("season_id"), rs.getString("season_name"),
                rs.getString("team_represented_id"), rs.getString("team_name"), rs.getString("opponent_team_id"), rs.getString("opponent_team_name"),
                rs.getString("player1_id"), rs.getString("player1_name"), rs.getString("player2_id"), rs.getString("player2_name"),
                rs.getInt("innings_number"), rs.getInt("partnership_number"), rs.getInt("runs_scored"), rs.getInt("balls_faced"),
                rs.getInt("player1_runs"), rs.getInt("player1_balls_faced"), rs.getInt("player2_runs"), rs.getInt("player2_balls_faced"),
                rs.getBoolean("partnership_broken"), rs.getString("who_got_out_id"), rs.getBoolean("match_won"),
                rs.getTimestamp("completed_at") == null ? null : rs.getTimestamp("completed_at").toLocalDateTime(),
                rs.getInt("fours_hit"), rs.getInt("sixes_hit"),
                rs.getInt("player1_fours_hit"), rs.getInt("player1_sixes_hit"),
                rs.getInt("player2_fours_hit"), rs.getInt("player2_sixes_hit")
        );
    }

    private String partnershipWhere(MapSqlParameterSource params, PartnershipStatsFilter filter) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (hasText(filter.seasonId())) { where.append(" AND pp.season_id = :seasonId"); params.addValue("seasonId", filter.seasonId().trim()); }
        if (filter.matchType() != null) { where.append(" AND pp.match_type = :matchType"); params.addValue("matchType", filter.matchType().name()); }
        if (hasText(filter.teamId())) { where.append(" AND pp.team_represented_id = :teamId"); params.addValue("teamId", filter.teamId().trim()); }
        if (hasText(filter.opponentTeamId())) {
            where.append(" AND (CASE WHEN m.team_a_id = pp.team_represented_id THEN m.team_b_id ELSE m.team_a_id END) = :opponentTeamId");
            params.addValue("opponentTeamId", filter.opponentTeamId().trim());
        }
        if (filter.inningsNumber() != null) { where.append(" AND pp.innings_number = :inningsNumber"); params.addValue("inningsNumber", filter.inningsNumber()); }
        if (filter.partnershipNumber() != null) { where.append(" AND pp.partnership_number = :partnershipNumber"); params.addValue("partnershipNumber", filter.partnershipNumber()); }
        if (filter.battingFirst() != null) { where.append(" AND pp.batting_first = :battingFirst"); params.addValue("battingFirst", filter.battingFirst()); }
        if (hasText(filter.playerId()) && hasText(filter.partnerId())) {
            where.append(" AND ((pp.player1_id = :playerId AND pp.player2_id = :partnerId) OR (pp.player1_id = :partnerId AND pp.player2_id = :playerId))");
            params.addValue("playerId", filter.playerId().trim());
            params.addValue("partnerId", filter.partnerId().trim());
        } else if (hasText(filter.playerId())) {
            where.append(" AND (pp.player1_id = :playerId OR pp.player2_id = :playerId)");
            params.addValue("playerId", filter.playerId().trim());
        } else if (hasText(filter.partnerId())) {
            where.append(" AND (pp.player1_id = :partnerId OR pp.player2_id = :partnerId)");
            params.addValue("partnerId", filter.partnerId().trim());
        }
        if (filter.result() != null) {
            where.append(switch (filter.result()) {
                case WIN -> " AND pp.match_won = TRUE";
                case LOSS -> " AND pp.match_won = FALSE AND COALESCE(m.is_match_tied, FALSE) = FALSE AND COALESCE(m.is_match_drawn, FALSE) = FALSE AND m.winner_team_id IS NOT NULL";
                case TIE -> " AND COALESCE(m.is_match_tied, FALSE) = TRUE";
                case NO_RESULT -> " AND (COALESCE(m.is_match_drawn, FALSE) = TRUE OR (m.winner_team_id IS NULL AND COALESCE(m.is_match_tied, FALSE) = FALSE))";
            });
        }
        return where.toString();
    }

    private String rivalryWhere(MapSqlParameterSource params, RivalryStatsFilter filter) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (hasText(filter.seasonId())) { where.append(" AND pr.season_id = :seasonId"); params.addValue("seasonId", filter.seasonId().trim()); }
        if (filter.matchType() != null) { where.append(" AND pr.match_type = :matchType"); params.addValue("matchType", filter.matchType().name()); }
        if (hasText(filter.teamId())) { where.append(" AND bpm.team_represented_id = :teamId"); params.addValue("teamId", filter.teamId().trim()); }
        if (hasText(filter.opponentTeamId())) { where.append(" AND bpm.opposition_team_id = :opponentTeamId"); params.addValue("opponentTeamId", filter.opponentTeamId().trim()); }
        if (filter.inningsNumber() != null) { where.append(" AND pr.innings_number = :inningsNumber"); params.addValue("inningsNumber", filter.inningsNumber()); }
        if (hasText(filter.batsmanId())) { where.append(" AND pr.batsman_id = :batsmanId"); params.addValue("batsmanId", filter.batsmanId().trim()); }
        if (hasText(filter.bowlerId())) { where.append(" AND pr.bowler_id = :bowlerId"); params.addValue("bowlerId", filter.bowlerId().trim()); }
        if (filter.matchResult() != null) {
            where.append(switch (filter.matchResult()) {
                case WIN -> " AND bpm.match_won = TRUE";
                case LOSS -> " AND bpm.match_won = FALSE AND COALESCE(m.is_match_tied, FALSE) = FALSE AND COALESCE(m.is_match_drawn, FALSE) = FALSE AND m.winner_team_id IS NOT NULL";
                case TIE -> " AND COALESCE(m.is_match_tied, FALSE) = TRUE";
                case NO_RESULT -> " AND (COALESCE(m.is_match_drawn, FALSE) = TRUE OR (m.winner_team_id IS NULL AND COALESCE(m.is_match_tied, FALSE) = FALSE))";
            });
        }
        return where.toString();
    }

    private static DismissalType dismissalType(String value) {
        return value == null ? null : DismissalType.valueOf(value);
    }


    static String buildBattingSql(String where, String orderBy) {
        return buildBattingSql(where, orderBy, "COUNT(DISTINCT pm.match_id)");
    }

    static String buildBattingSql(String where, String orderBy, String matchesPlayedExpression) {
        return """
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
                    %s AS matches_played,
                    COUNT(*) AS innings_played,
                    COALESCE(SUM(pm.dot_balls_played), 0) AS dot_balls,
                    CASE
                        WHEN COALESCE(SUM(pm.balls_faced), 0) = 0 THEN 0
                        ELSE SUM(pm.runs_scored)::double precision * 100.0 / SUM(pm.balls_faced)
                    END AS strike_rate,
                    CASE
                        WHEN SUM(CASE WHEN pm.out = TRUE THEN 1 ELSE 0 END) = 0
                            THEN NULL
                        ELSE SUM(pm.runs_scored)::double precision /
                             SUM(CASE WHEN pm.out = TRUE THEN 1 ELSE 0 END)
                    END AS batting_average
                FROM player_matches pm
                JOIN players p ON p.id = pm.player_id
                JOIN matches m ON m.id = pm.match_id
                WHERE pm.batted = TRUE
                %s
                GROUP BY pm.player_id, p.name
                HAVING COUNT(*) >= :minInnings
                ORDER BY %s
                LIMIT :limit
                """.formatted(matchesPlayedExpression, where == null ? "" : where, orderBy);
    }

    static String buildBowlingSql(String where, String orderBy) {
        return buildBowlingSql(where, orderBy, "COUNT(DISTINCT match_id)");
    }

    static String buildBowlingSql(String where, String orderBy, String matchesPlayedExpression) {
        return """
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
                    %s
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
                        %s AS matches_played,
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
                    FROM filtered f
                    GROUP BY f.player_id
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
                ORDER BY %s
                LIMIT :limit
                """.formatted(where == null ? "" : where, matchesPlayedExpression, orderBy);
    }

    static String buildFieldingSql(String where, String orderBy) {
        return buildFieldingSql(where, orderBy, "COUNT(DISTINCT pm.match_id)");
    }

    static String buildFieldingSql(String where, String orderBy, String matchesPlayedExpression) {
        return """
                SELECT
                    pm.player_id AS player_id,
                    p.name AS player_name,
                    COALESCE(SUM(pm.catches_taken), 0) AS total_catches,
                    COALESCE(SUM(pm.run_outs), 0) AS total_run_outs,
                    COALESCE(SUM(pm.stumpings), 0) AS total_stumpings,
                    %s AS matches_played,
                    COUNT(DISTINCT CASE WHEN pm.player_of_the_match = TRUE THEN pm.match_id END) AS motm_awards
                FROM player_matches pm
                JOIN players p ON p.id = pm.player_id
                JOIN matches m ON m.id = pm.match_id
                WHERE 1 = 1
                %s
                GROUP BY pm.player_id, p.name
                ORDER BY %s
                LIMIT :limit
                """.formatted(matchesPlayedExpression, where == null ? "" : where, orderBy);
    }

    private String participationCountExpression(
            String seasonId, MatchType matchType, String teamId, String opponentTeamId, MatchResult result,
            String playerReference
    ) {
        StringBuilder sql = new StringBuilder("(SELECT COUNT(DISTINCT mpp.match_id) FROM match_player_participation mpp JOIN matches mpp_m ON mpp_m.id = mpp.match_id WHERE mpp.player_id = ")
                .append(playerReference);
        if (hasText(seasonId)) sql.append(" AND mpp.season_id = :seasonId");
        if (matchType != null) sql.append(" AND mpp.match_type = :matchType");
        if (hasText(teamId)) sql.append(" AND mpp.team_represented_id = :teamId");
        if (hasText(opponentTeamId)) sql.append(" AND mpp.opposition_team_id = :opponentTeamId");
        if (result != null) {
            sql.append(switch (result) {
                case WIN -> " AND mpp.match_won = TRUE AND COALESCE(mpp_m.is_match_tied, FALSE) = FALSE AND COALESCE(mpp_m.is_match_drawn, FALSE) = FALSE";
                case LOSS -> " AND mpp.match_won = FALSE AND COALESCE(mpp_m.is_match_tied, FALSE) = FALSE AND COALESCE(mpp_m.is_match_drawn, FALSE) = FALSE AND mpp_m.winner_team_id IS NOT NULL";
                case TIE -> " AND COALESCE(mpp_m.is_match_tied, FALSE) = TRUE";
                case NO_RESULT -> " AND (COALESCE(mpp_m.is_match_drawn, FALSE) = TRUE OR (mpp_m.winner_team_id IS NULL AND COALESCE(mpp_m.is_match_tied, FALSE) = FALSE))";
            });
        }
        return sql.append(")").toString();
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