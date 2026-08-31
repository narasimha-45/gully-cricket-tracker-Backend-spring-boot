package com.gullycricket.backend.matches.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gullycricket.backend.matches.dto.*;
import com.gullycricket.backend.matches.entity.Match;
import com.gullycricket.backend.matches.entity.MatchType;
import com.gullycricket.backend.matches.repository.MatchRepository;
import com.gullycricket.backend.matches.repository.MatchProjectionMaintenanceRepository;
import com.gullycricket.backend.seasons.entity.Season;
import com.gullycricket.backend.seasons.repository.SeasonRepository;
import com.gullycricket.backend.seasons.service.SeasonService;
import com.gullycricket.backend.teams.entity.Team;
import com.gullycricket.backend.teams.service.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock MatchRepository matchRepository;
    @Mock SeasonRepository seasonRepository;
    @Mock ProcessPlayerStatsService processPlayerStatsService;
    @Mock TeamService teamService;
    @Mock SeasonService seasonService;
    @Mock MatchProjectionMaintenanceRepository maintenanceRepository;

    private MatchService matchService;
    private Season season;

    @BeforeEach
    void setUp() {
        matchService = new MatchService(
                matchRepository,
                seasonRepository,
                new ObjectMapper(),
                processPlayerStatsService,
                teamService,
                new MatchValidator(),
                seasonService,
                maintenanceRepository
        );
        season = new Season();
        season.setId("season-1");
        season.setSeasonName("S1");
        season.setMatchesPlayed(0);

        lenient().when(seasonRepository.findById("season-1")).thenReturn(Optional.of(season));
        lenient().when(teamService.getTeamByName(anyString())).thenReturn(null);
        lenient().when(teamService.getTeamsByNames(any())).thenReturn(List.of());
        lenient().when(teamService.saveTeam(any(Team.class))).thenAnswer(invocation -> {
            Team team = invocation.getArgument(0);
            if (team.getId() == null) {
                team.setId(team.getTeamName() + "-id");
            }
            return team;
        });
        lenient().when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> {
            Match match = invocation.getArgument(0);
            match.setId("match-1");
            return match;
        });
    }

    @Test
    void createsTieWithoutNullWinnerFailureAndStoresEveryRegularInnings() {
        matchService.saveMatch(limitedOvers(null), null);

        ArgumentCaptor<Match> captor = ArgumentCaptor.forClass(Match.class);
        verify(matchRepository).save(captor.capture());
        Match saved = captor.getValue();

        assertThat(saved.getIsMatchTied()).isTrue();
        assertThat(saved.getWinnerTeam()).isNull();
        assertThat(saved.getInningsSummaries()).hasSize(2);
        assertThat(saved.getInningsSummaries().get(0).getSequenceNumber()).isEqualTo(1);
        assertThat(saved.getBattingFirstTeam().getTeamName()).isEqualTo("eagles");
        verify(seasonService).syncMatchesPlayed("season-1");
        verify(maintenanceRepository).acquireProjectionWriteLock();
    }

    @Test
    void testDrawUsesInningsSummariesInsteadOfFlatScores() {
        MatchDataDto dto = new MatchDataDto(
                "season-1",
                teams(),
                new TossDto("Eagles", "BAT"),
                rules(),
                0,
                MatchType.TEST,
                null,
                List.of(
                        innings("Eagles", "Spiders", 180, 10, 420),
                        innings("Spiders", "Eagles", 160, 10, 390),
                        innings("Eagles", "Spiders", 120, 4, 240),
                        innings("Spiders", "Eagles", 100, 3, 180)
                ),
                new ResultDto(null, "DRAW", 0, null)
        );

        matchService.saveMatch(dto, null);
        ArgumentCaptor<Match> captor = ArgumentCaptor.forClass(Match.class);
        verify(matchRepository).save(captor.capture());
        Match saved = captor.getValue();

        assertThat(saved.getIsMatchDrawn()).isTrue();
        assertThat(saved.getInningsSummaries()).hasSize(4);
        assertThat(saved.getTeamAScore()).isZero(); // legacy flat fields intentionally unused for Test
    }

    @Test
    void replayUsesStoredJsonResetsStaleDerivedFieldsAndPreservesMatchIdentityMetadata() {
        Match match = new Match();
        match.setId("match-1");
        match.setSeason(season);
        match.setCompletedAt(LocalDateTime.of(2026, 1, 2, 3, 4));
        match.setIdempotencyKey("original-key");
        match.setMatchData(new ObjectMapper().valueToTree(limitedOvers("Eagles")));

        // Simulate stale values produced by an older projection implementation.
        match.setIsMatchDrawn(true);
        match.setIsMatchTied(true);
        match.setWinByWickets(7);
        match.setWonBy("stale result");

        matchService.replayStoredMatch(match);

        assertThat(match.getId()).isEqualTo("match-1");
        assertThat(match.getCompletedAt()).isEqualTo(LocalDateTime.of(2026, 1, 2, 3, 4));
        assertThat(match.getIdempotencyKey()).isEqualTo("original-key");
        assertThat(match.getIsMatchDrawn()).isFalse();
        assertThat(match.getIsMatchTied()).isFalse();
        assertThat(match.getWinByWickets()).isNull();
        assertThat(match.getWinByRuns()).isEqualTo(10);
        assertThat(match.getWonBy()).isEqualTo("eagles won by 10 runs");
        assertThat(match.getInningsSummaries()).hasSize(2);
        verify(processPlayerStatsService).processPlayerStats(eq(match), any(MatchDataDto.class));
    }

    @Test
    void idempotentRetryReturnsExistingMatchWithoutWritingAgain() {
        Match existing = new Match();
        existing.setId("existing-match");
        existing.setSeason(season);
        existing.setMatchData(new ObjectMapper().createObjectNode());
        when(matchRepository.findByIdempotencyKey("retry-key")).thenReturn(Optional.of(existing));

        MatchResponseDto response = matchService.saveMatch(limitedOvers("Eagles"), "retry-key");

        assertThat(response.id()).isEqualTo("existing-match");
        verify(matchRepository, never()).save(any());
        verifyNoInteractions(processPlayerStatsService);
    }

    private MatchDataDto limitedOvers(String winner) {
        return new MatchDataDto(
                "season-1", teams(), new TossDto("Eagles", "BAT"), rules(), 10, MatchType.OVERS, null,
                List.of(
                        innings("EAGLES", "spiders", 100, 2, 60),
                        innings("Spiders", "Eagles", winner == null ? 100 : 90, 4, 60)
                ),
                new ResultDto(winner, winner == null ? "TIE" : "RUNS", winner == null ? 0 : 10, "Alice")
        );
    }

    private Map<String, TeamDto> teams() {
        return Map.of(
                "teamA", new TeamDto(" Eagles ", List.of("Alice", "Amy")),
                "teamB", new TeamDto("Spiders", List.of("Bob", "Ben"))
        );
    }

    private InningsDto innings(String batting, String bowling, int runs, int wickets, int balls) {
        return new InningsDto(
                batting, bowling, 1, runs, wickets, balls,
                Map.of(), Map.of(), new ExtrasDto(0, 0), Map.of(), List.of(), false, false, true, null
        );
    }

    private RulesDto rules() {
        return new RulesDto(new RuleDetailDto(true, true), new RuleDetailDto(true, true));
    }
}
