package com.gullycricket.backend.seasons.service;

import com.gullycricket.backend.matches.entity.MatchStatus;
import com.gullycricket.backend.matches.repository.read.MatchInningsBreakdownRow;
import com.gullycricket.backend.matches.repository.read.MatchSummaryReadRepository;
import com.gullycricket.backend.matches.repository.read.MatchSummaryRow;
import com.gullycricket.backend.seasons.dto.MatchResponseDto;
import com.gullycricket.backend.seasons.repository.SeasonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeasonServiceTest {

    @Mock MatchSummaryReadRepository matchSummaryReadRepository;
    @Mock SeasonRepository seasonRepository;

    private SeasonService seasonService;

    @BeforeEach
    void setUp() {
        seasonService = new SeasonService(seasonRepository, matchSummaryReadRepository);
        lenient().when(seasonRepository.existsById("season-1")).thenReturn(true);
    }

    @Test
    void attachesPerInningsBreakdownKeyedToWhicheverTeamBattedFirst() {
        // team B is the raw team_a_id/team_b_id "team A", but batted second — the DTO's teamA/teamB
        // fields mean "batted first"/"batted second", so the breakdown must follow that same swap.
        MatchSummaryRow row = new MatchSummaryRow(
                "match-1", "season-1", "Season 1",
                "team-a-id", "Eagles", 120, 4, 60,
                "team-b-id", "Spiders", 350, 6, 600,
                "team-b-id", // battingFirstTeamId — team B batted first
                "team-b-id", "Spiders",
                false, true, false,
                "Spiders won by an innings and 90 runs",
                null, MatchStatus.COMPLETED, 0
        );
        when(matchSummaryReadRepository.findBySeasonId("season-1")).thenReturn(List.of(row));
        when(matchSummaryReadRepository.findInningsBreakdownBySeasonId("season-1")).thenReturn(List.of(
                new MatchInningsBreakdownRow("match-1", "team-b-id", 1, 350, 6, 600, true, false, "DECLARED"),
                new MatchInningsBreakdownRow("match-1", "team-a-id", 1, 120, 10, 300, true, false, "ALL_OUT"),
                new MatchInningsBreakdownRow("match-1", "team-a-id", 2, 140, 10, 250, true, true, "ALL_OUT")
        ));

        List<MatchResponseDto> result = seasonService.getAllMatchesBySeasonId("season-1");

        assertThat(result).hasSize(1);
        MatchResponseDto dto = result.getFirst();

        // dto.teamA == "batted first" == raw team B here
        assertThat(dto.teamA()).isEqualTo("Spiders");
        assertThat(dto.teamAInnings()).hasSize(1);
        assertThat(dto.teamAInnings().getFirst().completionReason()).isEqualTo("DECLARED");

        assertThat(dto.teamB()).isEqualTo("Eagles");
        assertThat(dto.teamBInnings()).hasSize(2);
        assertThat(dto.teamBInnings().get(1).followOn()).isTrue();
        assertThat(dto.teamBInnings().get(1).completionReason()).isEqualTo("ALL_OUT");
    }

    @Test
    void limitedOversMatchWithNoBreakdownRowsGetsEmptyInningsLists() {
        MatchSummaryRow row = new MatchSummaryRow(
                "match-2", "season-1", "Season 1",
                "team-a-id", "Eagles", 100, 2, 60,
                "team-b-id", "Spiders", 90, 5, 60,
                "team-a-id",
                "team-a-id", "Eagles",
                false, false, false,
                "Eagles won by 10 runs",
                null, MatchStatus.COMPLETED, 10
        );
        when(matchSummaryReadRepository.findBySeasonId("season-1")).thenReturn(List.of(row));
        when(matchSummaryReadRepository.findInningsBreakdownBySeasonId("season-1")).thenReturn(List.of());

        List<MatchResponseDto> result = seasonService.getAllMatchesBySeasonId("season-1");

        assertThat(result.getFirst().teamAInnings()).isEmpty();
        assertThat(result.getFirst().teamBInnings()).isEmpty();
    }
}
