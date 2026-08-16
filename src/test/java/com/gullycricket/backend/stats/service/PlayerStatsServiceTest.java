package com.gullycricket.backend.stats.service;

import com.gullycricket.backend.matches.entity.Match;
import com.gullycricket.backend.players.entity.Player;
import com.gullycricket.backend.players.entity.PlayerMatch;
import com.gullycricket.backend.players.repository.PlayerMatchRepository;
import com.gullycricket.backend.players.repository.PlayerRepository;
import com.gullycricket.backend.stats.dto.BowlingStatsFilter;
import com.gullycricket.backend.stats.dto.BowlingStatsResponse;
import com.gullycricket.backend.stats.enums.BowlingSortBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerStatsServiceTest {

    @Mock PlayerMatchRepository playerMatchRepository;
    @Mock PlayerRepository playerRepository;

    @Test
    void aggregatesTenWicketHaulAcrossMatchAndFormatsPartialOvers() {
        Player player = new Player();
        player.setId("p1");
        player.setName("bowler");

        Match match = new Match();
        match.setId("m1");

        PlayerMatch first = bowlingRow(player, match, 6, 30, 10);
        PlayerMatch second = bowlingRow(player, match, 4, 20, 9);

        when(playerMatchRepository.findAll(any(Specification.class))).thenReturn(List.of(first, second));
        PlayerStatsService service = new PlayerStatsService(playerMatchRepository, playerRepository);

        BowlingStatsFilter filter = new BowlingStatsFilter(null, null, null, null, null, null);
        List<BowlingStatsResponse> result = service.getBowlingLeaderboard(filter, BowlingSortBy.WICKETS, null, 10);

        assertThat(result).hasSize(1);
        BowlingStatsResponse stats = result.getFirst();
        assertThat(stats.totalWickets()).isEqualTo(10);
        assertThat(stats.tenWicketHauls()).isEqualTo(1);
        assertThat(stats.fiveWicketHauls()).isEqualTo(1);
        assertThat(stats.totalOversBowled()).isEqualTo(3.1);
        assertThat(stats.bestBowlingFigures().wickets()).isEqualTo(6);
        assertThat(stats.bestBowlingFigures().runsConceded()).isEqualTo(30);
    }

    private PlayerMatch bowlingRow(Player player, Match match, int wickets, int runs, int balls) {
        PlayerMatch row = new PlayerMatch();
        row.setPlayer(player);
        row.setMatch(match);
        row.setBowled(true);
        row.setWicketsTaken(wickets);
        row.setRunsConceded(runs);
        row.setBallsBowled(balls);
        return row;
    }
}
