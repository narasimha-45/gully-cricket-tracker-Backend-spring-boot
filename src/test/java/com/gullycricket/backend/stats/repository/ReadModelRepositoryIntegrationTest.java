package com.gullycricket.backend.stats.repository;

import com.gullycricket.backend.matches.repository.read.MatchSummaryReadRepository;
import com.gullycricket.backend.search.repository.SearchReadRepository;
import com.gullycricket.backend.stats.dto.BattingStatsFilter;
import com.gullycricket.backend.stats.dto.BowlingStatsFilter;
import com.gullycricket.backend.stats.enums.BattingSortBy;
import com.gullycricket.backend.stats.enums.BowlingSortBy;
import com.gullycricket.backend.teams.repository.read.TeamReadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class ReadModelRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.mongodb.repositories.enabled", () -> "false");
    }

    @Autowired StatsReadRepository statsReadRepository;
    @Autowired MatchSummaryReadRepository matchSummaryReadRepository;
    @Autowired SearchReadRepository searchReadRepository;
    @Autowired TeamReadRepository teamReadRepository;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM player_matches");
        jdbc.update("DELETE FROM match_innings_summary");
        jdbc.update("DELETE FROM matches");
        jdbc.update("DELETE FROM player_teams");
        jdbc.update("DELETE FROM season_players");
        jdbc.update("DELETE FROM team_season");
        jdbc.update("DELETE FROM players");
        jdbc.update("DELETE FROM teams");
        jdbc.update("DELETE FROM seasons");

        jdbc.update("INSERT INTO seasons(id, season_name, matches_played, created_at) VALUES ('s1','Season 1',1,CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO teams(id, team_name) VALUES ('ta','eagles'),('tb','spiders')");
        jdbc.update("INSERT INTO team_season(team_id, season_id) VALUES ('ta','s1'),('tb','s1')");
        jdbc.update("INSERT INTO players(id, name) VALUES ('p1','alice'),('p2','bob')");
        jdbc.update("""
                INSERT INTO matches(
                    id, season_id, status, match_type, team_a_id, team_b_id,
                    batting_first_team_id, batting_second_team_id, winner_team_id,
                    team_a_score, team_a_wickets, team_a_balls_faced,
                    team_b_score, team_b_wickets, team_b_balls_faced,
                    is_match_tied, is_match_drawn, super_over, completed_at
                ) VALUES (
                    'm1','s1','COMPLETED','OVERS','ta','tb','ta','tb','ta',
                    100,2,60,90,5,60,FALSE,FALSE,FALSE,CURRENT_TIMESTAMP
                )
                """);
        jdbc.update("""
                INSERT INTO match_innings_summary(
                    id, match_id, batting_team_id, bowling_team_id, sequence_number,
                    team_innings_number, runs, wickets, balls, super_over, completed
                ) VALUES
                    ('i1','m1','ta','tb',1,1,100,2,60,FALSE,TRUE),
                    ('i2','m1','tb','ta',2,1,90,5,60,FALSE,TRUE)
                """);
        jdbc.update("""
                INSERT INTO player_matches(
                    id, player_id, match_id, season_id, team_represented_id, opposition_team_id,
                    match_type, innings_number, match_won, player_of_the_match,
                    batted, batting_position, batting_first, runs_scored, balls_faced,
                    fours_hit, sixes_hit, out, dot_balls_played,
                    bowled, bowling_first, wickets_taken, balls_bowled, runs_conceded,
                    maidens_bowled, wides_bowled, no_balls_bowled,
                    bowled_dismissals, caught_dismissals, lbw_dismissals, stumped_dismissals,
                    hit_wicket_dismissals, special_wicket_dismissals, dot_balls_bowled,
                    catches_taken, run_outs, stumpings
                ) VALUES (
                    'pm1','p1','m1','s1','ta','tb','OVERS',1,TRUE,TRUE,
                    TRUE,1,TRUE,56,40,6,2,TRUE,10,
                    TRUE,FALSE,3,60,25,1,0,0,0,0,0,0,0,0,30,1,0,0
                )
                """);
    }

    @Test
    void leaderboardsAggregateInPostgresWithoutHydratingMatchJson() {
        var batting = statsReadRepository.findBattingLeaderboard(
                new BattingStatsFilter("s1", null, null, null, null, null, null),
                BattingSortBy.RUNS, null, 10);
        var bowling = statsReadRepository.findBowlingLeaderboard(
                new BowlingStatsFilter("s1", null, null, null, null, null),
                BowlingSortBy.WICKETS, null, 10);

        assertThat(batting).hasSize(1);
        assertThat(batting.getFirst().totalRuns()).isEqualTo(56);
        assertThat(bowling).hasSize(1);
        assertThat(bowling.getFirst().totalWickets()).isEqualTo(3);
        assertThat(bowling.getFirst().bestBowlingFigures().wickets()).isEqualTo(3);
    }

    @Test
    void globalSearchUsesOneCompactReadQuery() {
        var result = searchReadRepository.globalSearch("ali");
        assertThat(result.players()).hasSize(1);
        assertThat(result.players().getFirst().matchesPlayed()).isEqualTo(1);
    }

    @Test
    void seasonTeamsUseCompactReadQuery() {
        var teams = teamReadRepository.findBySeasonId("s1");
        assertThat(teams).extracting(team -> team.teamName()).containsExactly("eagles", "spiders");
    }

    @Test
    void seasonSummaryUsesCompactInningsProjection() {
        var matches = matchSummaryReadRepository.findBySeasonId("s1");
        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().teamARuns()).isEqualTo(100);
        assertThat(matches.getFirst().teamBRuns()).isEqualTo(90);
    }
}
