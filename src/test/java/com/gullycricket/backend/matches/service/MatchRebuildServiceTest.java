package com.gullycricket.backend.matches.service;

import com.gullycricket.backend.matches.dto.RebuildResultDto;
import com.gullycricket.backend.matches.entity.Match;
import com.gullycricket.backend.matches.repository.MatchProjectionMaintenanceRepository;
import com.gullycricket.backend.matches.repository.MatchRepository;
import com.gullycricket.backend.seasons.entity.Season;
import com.gullycricket.backend.seasons.repository.SeasonRepository;
import com.gullycricket.backend.seasons.service.SeasonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchRebuildServiceTest {

    @Mock MatchRepository matchRepository;
    @Mock SeasonRepository seasonRepository;
    @Mock MatchProjectionMaintenanceRepository maintenanceRepository;
    @Mock MatchService matchService;
    @Mock SeasonService seasonService;

    private MatchRebuildService service;
    private Season season;

    @BeforeEach
    void setUp() {
        service = new MatchRebuildService(
                matchRepository, seasonRepository, maintenanceRepository, matchService, seasonService);
        season = new Season();
        season.setId("season-1");
    }

    @Test
    void rebuildMatchOnlyReplacesMatchOwnedProjections() {
        Match match = match("match-1");
        when(matchRepository.findById("match-1")).thenReturn(Optional.of(match));

        RebuildResultDto result = service.rebuildMatch("match-1");

        verify(maintenanceRepository).acquireProjectionWriteLock();
        verify(maintenanceRepository).deleteMatchProjections("match-1");
        verify(maintenanceRepository, never()).deleteSeasonProjections(anyString());
        verify(matchService).replayStoredMatch(match);
        verify(seasonService).syncMatchesPlayed("season-1");
        assertThat(result.matchesReplayed()).isEqualTo(1);
        assertThat(result.scope()).isEqualTo("MATCH");
    }

    @Test
    void deletingScrapMatchRebuildsWholeRemainingSeason() {
        Match scrap = match("scrap");
        Match remaining1 = match("m1");
        Match remaining2 = match("m2");

        when(matchRepository.findById("scrap")).thenReturn(Optional.of(scrap));
        when(matchRepository.findIdsBySeasonIdForReplay("season-1")).thenReturn(List.of("m1", "m2"));
        when(matchRepository.findById("m1")).thenReturn(Optional.of(remaining1));
        when(matchRepository.findById("m2")).thenReturn(Optional.of(remaining2));

        RebuildResultDto result = service.deleteMatchAndRebuildSeason("scrap");

        verify(maintenanceRepository).acquireProjectionWriteLock();
        verify(matchRepository).delete(scrap);
        verify(matchRepository).flush();
        verify(maintenanceRepository).deleteSeasonProjections("season-1");
        verify(matchService).replayStoredMatch(remaining1);
        verify(matchService).replayStoredMatch(remaining2);
        verify(seasonService).syncMatchesPlayed("season-1");
        assertThat(result.matchesReplayed()).isEqualTo(2);
        assertThat(result.scope()).isEqualTo("DELETE_MATCH");
    }

    @Test
    void fullRebuildClearsAllProjectionsAndReplaysEveryMatch() {
        Match match1 = match("m1");
        Match match2 = match("m2");
        when(matchRepository.findAllIdsForReplay()).thenReturn(List.of("m1", "m2"));
        when(matchRepository.findById("m1")).thenReturn(Optional.of(match1));
        when(matchRepository.findById("m2")).thenReturn(Optional.of(match2));

        RebuildResultDto result = service.rebuildAll();

        verify(maintenanceRepository).acquireProjectionWriteLock();
        verify(maintenanceRepository).deleteAllProjections();
        verify(matchService).replayStoredMatch(match1);
        verify(matchService).replayStoredMatch(match2);
        verify(seasonService).syncAllMatchesPlayed();
        assertThat(result.matchesReplayed()).isEqualTo(2);
        assertThat(result.scope()).isEqualTo("ALL");
    }

    private Match match(String id) {
        Match match = new Match();
        match.setId(id);
        match.setSeason(season);
        return match;
    }
}
