package com.gullycricket.backend.stats.service;

import com.gullycricket.backend.players.repository.PlayerRepository;
import com.gullycricket.backend.stats.dto.BowlingStatsFilter;
import com.gullycricket.backend.stats.dto.BowlingStatsResponse;
import com.gullycricket.backend.stats.enums.BestBowlingFigures;
import com.gullycricket.backend.stats.enums.BowlingSortBy;
import com.gullycricket.backend.stats.repository.PlayerProfileReadRepository;
import com.gullycricket.backend.stats.repository.StatsReadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerStatsServiceTest {

    @Mock PlayerRepository playerRepository;
    @Mock StatsReadRepository statsReadRepository;
    @Mock PlayerProfileReadRepository playerProfileReadRepository;

    @Test
    void bowlingLeaderboardUsesReadOptimizedAggregateRepository() {
        BowlingStatsFilter filter = new BowlingStatsFilter(null, null, null, null, null, null);
        BowlingStatsResponse expected = new BowlingStatsResponse(
                "p1", "bowler", 10, 50, 15.79, 3.1, 0, 5.0,
                new BestBowlingFigures(6, 30, 10), 1, 1, 1, 2, 0
        );
        when(statsReadRepository.findBowlingLeaderboard(filter, BowlingSortBy.WICKETS, null, 10))
                .thenReturn(List.of(expected));

        PlayerStatsService service = new PlayerStatsService(playerRepository, statsReadRepository, playerProfileReadRepository);
        List<BowlingStatsResponse> result = service.getBowlingLeaderboard(filter, BowlingSortBy.WICKETS, null, 10);

        assertThat(result).containsExactly(expected);
    }
}
