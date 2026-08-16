package com.gullycricket.backend.teams.repository.read;

import com.gullycricket.backend.config.DbQueryTimer;
import com.gullycricket.backend.teams.dto.TeamSearchSuggestionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Lightweight team navigation reads that avoid JPA entity hydration. */
@Repository
@RequiredArgsConstructor
public class TeamReadRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final DbQueryTimer queryTimer;

    public List<TeamSearchSuggestionDto> findAll() {
        String sql = """
                SELECT t.id, t.team_name
                FROM teams t
                ORDER BY t.team_name ASC
                """;
        return queryTimer.record("teams.all", () -> jdbc.query(
                sql,
                new MapSqlParameterSource(),
                (rs, rowNum) -> new TeamSearchSuggestionDto(rs.getString("id"), rs.getString("team_name"))
        ));
    }

    public List<TeamSearchSuggestionDto> findBySeasonId(String seasonId) {
        String sql = """
                SELECT t.id, t.team_name
                FROM team_season ts
                JOIN teams t ON t.id = ts.team_id
                WHERE ts.season_id = :seasonId
                ORDER BY t.team_name ASC
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("seasonId", seasonId);
        return queryTimer.record("teams.bySeason", () -> jdbc.query(
                sql,
                params,
                (rs, rowNum) -> new TeamSearchSuggestionDto(rs.getString("id"), rs.getString("team_name"))
        ));
    }
}
