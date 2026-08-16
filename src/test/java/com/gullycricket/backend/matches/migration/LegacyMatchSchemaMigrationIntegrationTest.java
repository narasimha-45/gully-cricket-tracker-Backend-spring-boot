package com.gullycricket.backend.matches.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class LegacyMatchSchemaMigrationIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void v4RenamesLegacyHibernateMatchColumnsWithoutLosingValues() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement()) {

            statement.execute("""
                    CREATE TABLE matches (
                        id VARCHAR(255) PRIMARY KEY,
                        teamascore INTEGER,
                        teamawickets INTEGER,
                        teamaballs_faced INTEGER,
                        teambscore INTEGER,
                        teambwickets INTEGER,
                        teambballs_faced INTEGER
                    )
                    """);
            statement.execute("""
                    INSERT INTO matches(
                        id, teamascore, teamawickets, teamaballs_faced,
                        teambscore, teambwickets, teambballs_faced
                    ) VALUES ('legacy-match', 123, 4, 72, 119, 8, 70)
                    """);
        }

        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .baselineOnMigrate(true)
                .baselineVersion(MigrationVersion.fromVersion("3"))
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement()) {

            assertThat(columnExists(statement, "team_a_score")).isTrue();
            assertThat(columnExists(statement, "team_a_wickets")).isTrue();
            assertThat(columnExists(statement, "team_a_balls_faced")).isTrue();
            assertThat(columnExists(statement, "team_b_score")).isTrue();
            assertThat(columnExists(statement, "team_b_wickets")).isTrue();
            assertThat(columnExists(statement, "team_b_balls_faced")).isTrue();

            assertThat(columnExists(statement, "teamascore")).isFalse();
            assertThat(columnExists(statement, "teambscore")).isFalse();

            try (ResultSet result = statement.executeQuery("""
                    SELECT team_a_score, team_a_wickets, team_a_balls_faced,
                           team_b_score, team_b_wickets, team_b_balls_faced
                    FROM matches
                    WHERE id = 'legacy-match'
                    """)) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt("team_a_score")).isEqualTo(123);
                assertThat(result.getInt("team_a_wickets")).isEqualTo(4);
                assertThat(result.getInt("team_a_balls_faced")).isEqualTo(72);
                assertThat(result.getInt("team_b_score")).isEqualTo(119);
                assertThat(result.getInt("team_b_wickets")).isEqualTo(8);
                assertThat(result.getInt("team_b_balls_faced")).isEqualTo(70);
            }
        }
    }

    private boolean columnExists(Statement statement, String columnName) throws Exception {
        try (ResultSet result = statement.executeQuery("""
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.columns
                    WHERE table_schema = current_schema()
                      AND table_name = 'matches'
                      AND column_name = '%s'
                )
                """.formatted(columnName))) {
            result.next();
            return result.getBoolean(1);
        }
    }
}
