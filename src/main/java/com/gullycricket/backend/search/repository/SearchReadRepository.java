package com.gullycricket.backend.search.repository;

import com.gullycricket.backend.config.DbQueryTimer;
import com.gullycricket.backend.players.dto.PlayerSearchSuggestionDto;
import com.gullycricket.backend.search.dto.GlobalSearchResponseDto;
import com.gullycricket.backend.seasons.dto.SeasonSearchDto;
import com.gullycricket.backend.teams.dto.TeamSearchSuggestionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/** Search read model designed to minimize database round trips from typeahead UI. */
@Repository
@RequiredArgsConstructor
public class SearchReadRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final DbQueryTimer queryTimer;

    public GlobalSearchResponseDto globalSearch(String query) {
        MapSqlParameterSource params = new MapSqlParameterSource("pattern", "%" + query + "%");
        String sql = """
                WITH player_candidates AS (
                    SELECT p.id, p.name
                    FROM players p
                    WHERE p.name ILIKE :pattern
                    ORDER BY p.name
                    LIMIT 10
                ),
                player_hits AS (
                    SELECT
                        'PLAYER'::text AS kind,
                        p.id AS entity_id,
                        p.name AS entity_name,
                        COUNT(DISTINCT pm.match_id)::int AS metric
                    FROM player_candidates p
                    LEFT JOIN player_matches pm ON pm.player_id = p.id
                    GROUP BY p.id, p.name
                ),
                team_hits AS (
                    SELECT
                        'TEAM'::text AS kind,
                        t.id AS entity_id,
                        t.team_name AS entity_name,
                        0::int AS metric
                    FROM teams t
                    WHERE t.team_name ILIKE :pattern
                    ORDER BY t.team_name
                    LIMIT 10
                ),
                season_hits AS (
                    SELECT
                        'SEASON'::text AS kind,
                        s.id AS entity_id,
                        s.season_name AS entity_name,
                        COALESCE(s.matches_played, 0)::int AS metric
                    FROM seasons s
                    WHERE s.season_name ILIKE :pattern
                    ORDER BY s.season_name
                    LIMIT 10
                )
                SELECT kind, entity_id, entity_name, metric FROM player_hits
                UNION ALL
                SELECT kind, entity_id, entity_name, metric FROM team_hits
                UNION ALL
                SELECT kind, entity_id, entity_name, metric FROM season_hits
                ORDER BY kind, entity_name
                """;

        List<SearchRow> rows = queryTimer.record("search.global", () -> jdbc.query(sql, params,
                (rs, rowNum) -> new SearchRow(
                        rs.getString("kind"),
                        rs.getString("entity_id"),
                        rs.getString("entity_name"),
                        rs.getInt("metric")
                )));

        List<PlayerSearchSuggestionDto> players = new ArrayList<>();
        List<TeamSearchSuggestionDto> teams = new ArrayList<>();
        List<SeasonSearchDto> seasons = new ArrayList<>();
        for (SearchRow row : rows) {
            switch (row.kind()) {
                case "PLAYER" -> players.add(new PlayerSearchSuggestionDto(row.id(), row.name(), row.metric()));
                case "TEAM" -> teams.add(new TeamSearchSuggestionDto(row.id(), row.name()));
                case "SEASON" -> seasons.add(new SeasonSearchDto(row.id(), row.name(), row.metric()));
                default -> throw new IllegalStateException("Unexpected global search row type: " + row.kind());
            }
        }
        return new GlobalSearchResponseDto(List.copyOf(players), List.copyOf(teams), List.copyOf(seasons));
    }

    public List<PlayerSearchSuggestionDto> searchPlayers(String query) {
        MapSqlParameterSource params = new MapSqlParameterSource("pattern", "%" + query + "%");
        String sql = """
                WITH candidates AS (
                    SELECT p.id, p.name
                    FROM players p
                    WHERE p.name ILIKE :pattern
                    ORDER BY p.name
                    LIMIT 10
                )
                SELECT
                    p.id AS player_id,
                    p.name AS player_name,
                    COUNT(DISTINCT pm.match_id)::int AS matches_played
                FROM candidates p
                LEFT JOIN player_matches pm ON pm.player_id = p.id
                GROUP BY p.id, p.name
                ORDER BY p.name
                """;
        return queryTimer.record("search.players", () -> jdbc.query(sql, params,
                (rs, rowNum) -> new PlayerSearchSuggestionDto(
                        rs.getString("player_id"),
                        rs.getString("player_name"),
                        rs.getInt("matches_played")
                )));
    }

    private record SearchRow(String kind, String id, String name, int metric) {
    }
}
