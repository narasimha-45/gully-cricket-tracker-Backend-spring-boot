package com.gullycricket.backend.stats.repository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StatsReadRepositorySqlTest {

    @Test
    void battingSqlKeepsDynamicClausesSeparated() {
        String sql = normalizeSql(StatsReadRepository.buildBattingSql(
                " AND pm.season_id = :seasonId",
                "total_runs DESC, player_name ASC"));

        assertThat(sql)
                .contains("WHERE pm.batted = TRUE AND pm.season_id = :seasonId GROUP BY pm.player_id, p.name")
                .contains("ORDER BY total_runs DESC, player_name ASC")
                .doesNotContain(":seasonIdGROUP")
                .doesNotContain("ORDER BYtotal_runs");
    }

    @Test
    void bowlingSqlKeepsDynamicClausesSeparated() {
        String sql = normalizeSql(StatsReadRepository.buildBowlingSql(
                " AND pm.season_id = :seasonId",
                "total_wickets DESC, bowling_average ASC NULLS LAST, player_name ASC"));

        assertThat(sql)
                .contains("WHERE pm.bowled = TRUE AND pm.season_id = :seasonId ), aggregate_stats AS (")
                .contains("), ranked_figures AS (")
                .contains("), ten_haul AS (")
                .contains("ORDER BY total_wickets DESC, bowling_average ASC NULLS LAST, player_name ASC")
                .doesNotContain(":seasonIdGROUP")
                .doesNotContain("ORDER BYtotal_wickets");
    }

    @Test
    void fieldingSqlKeepsDynamicClausesSeparated() {
        String sql = normalizeSql(StatsReadRepository.buildFieldingSql(
                " AND pm.season_id = :seasonId",
                "total_catches DESC, player_name ASC"));

        assertThat(sql)
                .contains("WHERE 1 = 1 AND pm.season_id = :seasonId GROUP BY pm.player_id, p.name")
                .contains("ORDER BY total_catches DESC, player_name ASC")
                .doesNotContain(":seasonIdGROUP")
                .doesNotContain("ORDER BYtotal_catches");
    }

    private static String normalizeSql(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
