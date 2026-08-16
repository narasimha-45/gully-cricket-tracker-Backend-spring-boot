package com.gullycricket.backend.stats.repository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StatsReadRepositorySqlTest {

    @Test
    void battingSqlKeepsDynamicClausesSeparated() {
        String sql = StatsReadRepository.buildBattingSql(
                " AND pm.season_id = :seasonId",
                "total_runs DESC, player_name ASC");

        assertThat(sql)
                .contains(":seasonId\n")
                .contains("\nGROUP BY pm.player_id, p.name")
                .contains("ORDER BY total_runs DESC, player_name ASC")
                .doesNotContain(":seasonIdGROUP")
                .doesNotContain("ORDER BYtotal_runs");
    }

    @Test
    void bowlingSqlKeepsDynamicClausesSeparated() {
        String sql = StatsReadRepository.buildBowlingSql(
                " AND pm.season_id = :seasonId",
                "total_wickets DESC, bowling_average ASC NULLS LAST, player_name ASC");

        assertThat(sql)
                .contains(":seasonId\n")
                .contains("\n                ),")
                .contains("ORDER BY total_wickets DESC, bowling_average ASC NULLS LAST, player_name ASC")
                .doesNotContain(":seasonId)")
                .doesNotContain("ORDER BYtotal_wickets");
    }

    @Test
    void fieldingSqlKeepsDynamicClausesSeparated() {
        String sql = StatsReadRepository.buildFieldingSql(
                " AND pm.season_id = :seasonId",
                "total_catches DESC, player_name ASC");

        assertThat(sql)
                .contains(":seasonId\n")
                .contains("ORDER BY total_catches DESC, player_name ASC")
                .doesNotContain(":seasonIdGROUP")
                .doesNotContain("ORDER BYtotal_catches");
    }
}
