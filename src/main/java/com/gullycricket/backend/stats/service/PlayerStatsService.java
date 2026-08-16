package com.gullycricket.backend.stats.service;

import com.gullycricket.backend.common.exception.ResourceNotFoundException;
import com.gullycricket.backend.players.entity.Player;
import com.gullycricket.backend.players.repository.PlayerRepository;
import com.gullycricket.backend.stats.dto.*;
import com.gullycricket.backend.stats.enums.BattingSortBy;
import com.gullycricket.backend.stats.enums.BestBowlingFigures;
import com.gullycricket.backend.stats.enums.BowlingSortBy;
import com.gullycricket.backend.stats.enums.FieldingSortBy;
import com.gullycricket.backend.stats.enums.MatchResult;
import com.gullycricket.backend.stats.repository.PlayerProfileReadRepository;
import com.gullycricket.backend.stats.repository.PlayerStatReadRow;
import com.gullycricket.backend.stats.repository.StatsReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlayerStatsService {

    private final PlayerRepository playerRepository;
    private final StatsReadRepository statsReadRepository;
    private final PlayerProfileReadRepository playerProfileReadRepository;

    // Leaderboards are aggregated in PostgreSQL and never hydrate Match.matchData.
    public List<BattingStatsResponse> getBattingLeaderboard(BattingStatsFilter filter, BattingSortBy sortBy, Integer minInnings, Integer limit) {
        return statsReadRepository.findBattingLeaderboard(filter, sortBy, minInnings, limit);
    }

    public List<BowlingStatsResponse> getBowlingLeaderboard(BowlingStatsFilter filter, BowlingSortBy sortBy, Integer minInnings, Integer limit) {
        return statsReadRepository.findBowlingLeaderboard(filter, sortBy, minInnings, limit);
    }

    public List<FieldingAndMiscStatsResponse> getFieldingLeaderboard(FieldingAndMiscStatsFilter filter, FieldingSortBy sortBy, Integer limit) {
        return statsReadRepository.findFieldingLeaderboard(filter, sortBy, limit);
    }

    public PlayerProfileDto getPlayerProfile(String playerId, String seasonId) {
        List<PlayerStatReadRow> rows = playerProfileReadRepository.findRows(playerId, seasonId);
        String name;
        if (rows.isEmpty()) {
            Player player = playerRepository.findById(playerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + playerId));
            name = player.getName();
        } else {
            name = rows.getFirst().playerName();
        }

        Set<String> matchIds = distinctMatches(rows);
        Set<String> wonMatchIds = rows.stream().filter(PlayerStatReadRow::matchWon).map(PlayerStatReadRow::matchId).collect(Collectors.toSet());
        int motm = (int) rows.stream().filter(PlayerStatReadRow::playerOfTheMatch).map(PlayerStatReadRow::matchId).distinct().count();

        return new PlayerProfileDto(
                playerId,
                name,
                matchIds.size(),
                wonMatchIds.size(),
                round2(matchIds.isEmpty() ? 0 : wonMatchIds.size() * 100.0 / matchIds.size()),
                motm,
                computeBatting(playerId, name, rows),
                computeBowling(playerId, name, rows),
                computeFielding(playerId, name, rows),
                getRecentForm(rows, 3),
                getByBattingPosition(rows),
                getByInnings(rows),
                getByMatchResult(rows),
                getBySeason(rows),
                getByTeam(rows)
        );
    }

    private List<RecentInningDto> getRecentForm(List<PlayerStatReadRow> rows, int count) {
        return rows.stream()
                .filter(PlayerStatReadRow::batted)
                .sorted(Comparator.comparing(PlayerStatReadRow::completedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(count)
                .map(row -> new RecentInningDto(
                        row.matchId(), row.seasonId(), row.seasonName(),
                        row.teamId(), row.teamName(), row.opponentTeamId(), row.opponentTeamName(),
                        row.battingPosition(), row.runsScored(), row.ballsFaced(), row.foursHit(), row.sixesHit(),
                        row.out(), row.matchWon(), row.completedAt()
                ))
                .toList();
    }

    private List<BattingPositionStatsDto> getByBattingPosition(List<PlayerStatReadRow> rows) {
        return rows.stream()
                .filter(PlayerStatReadRow::batted)
                .filter(row -> row.battingPosition() != null)
                .collect(Collectors.groupingBy(PlayerStatReadRow::battingPosition))
                .entrySet().stream()
                .map(entry -> computePositionStats(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(BattingPositionStatsDto::battingPosition))
                .toList();
    }

    private BattingPositionStatsDto computePositionStats(Integer position, List<PlayerStatReadRow> rows) {
        int innings = rows.size();
        int notOuts = (int) rows.stream().filter(row -> !row.out()).count();
        int outs = innings - notOuts;
        int runs = sum(rows, PlayerStatReadRow::runsScored);
        int balls = sum(rows, PlayerStatReadRow::ballsFaced);
        int highest = rows.stream().mapToInt(PlayerStatReadRow::runsScored).max().orElse(0);
        return new BattingPositionStatsDto(
                position, innings, notOuts, runs, balls,
                round2(outs == 0 ? runs : (double) runs / outs),
                round2(balls == 0 ? 0 : runs * 100.0 / balls),
                highest,
                sum(rows, PlayerStatReadRow::foursHit),
                sum(rows, PlayerStatReadRow::sixesHit)
        );
    }

    private List<PlayerSplitStatsDto> getByInnings(List<PlayerStatReadRow> rows) {
        return rows.stream()
                .collect(Collectors.groupingBy(PlayerStatReadRow::inningsNumber))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.nullsLast(Comparator.naturalOrder())))
                .map(entry -> computeSplit("innings_" + entry.getKey(), "Innings " + entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<PlayerSplitStatsDto> getByMatchResult(List<PlayerStatReadRow> rows) {
        Map<MatchResult, List<PlayerStatReadRow>> grouped = rows.stream().collect(Collectors.groupingBy(this::resultOf));
        return Arrays.stream(MatchResult.values())
                .filter(grouped::containsKey)
                .map(result -> computeSplit(result.name(), resultLabel(result), grouped.get(result)))
                .toList();
    }

    private MatchResult resultOf(PlayerStatReadRow row) {
        if (row.matchTied()) return MatchResult.TIE;
        if (row.matchDrawn() || row.winnerTeamId() == null) return MatchResult.NO_RESULT;
        return row.matchWon() ? MatchResult.WIN : MatchResult.LOSS;
    }

    private String resultLabel(MatchResult result) {
        return switch (result) {
            case WIN -> "Won";
            case LOSS -> "Lost";
            case TIE -> "Tied";
            case NO_RESULT -> "No Result";
        };
    }

    private PlayerSplitStatsDto computeSplit(String key, String label, List<PlayerStatReadRow> rows) {
        Set<String> matchIds = distinctMatches(rows);
        int wins = (int) rows.stream().filter(PlayerStatReadRow::matchWon).map(PlayerStatReadRow::matchId).distinct().count();
        List<PlayerStatReadRow> batted = rows.stream().filter(PlayerStatReadRow::batted).toList();
        List<PlayerStatReadRow> bowled = rows.stream().filter(PlayerStatReadRow::bowled).toList();

        int runs = sum(batted, PlayerStatReadRow::runsScored);
        int balls = sum(batted, PlayerStatReadRow::ballsFaced);
        int outs = (int) batted.stream().filter(PlayerStatReadRow::out).count();
        int highest = batted.stream().mapToInt(PlayerStatReadRow::runsScored).max().orElse(0);
        int wickets = sum(bowled, PlayerStatReadRow::wicketsTaken);
        int conceded = sum(bowled, PlayerStatReadRow::runsConceded);
        int ballsBowled = sum(bowled, PlayerStatReadRow::ballsBowled);

        return new PlayerSplitStatsDto(
                key, label, matchIds.size(), wins,
                batted.size(), runs, balls,
                round2(outs == 0 ? runs : (double) runs / outs),
                round2(balls == 0 ? 0 : runs * 100.0 / balls), highest,
                bowled.size(), wickets, conceded,
                round2(ballsBowled == 0 ? 0 : conceded * 6.0 / ballsBowled),
                round2(wickets == 0 ? 0 : (double) conceded / wickets)
        );
    }

    private List<SeasonPlayerStatsDto> getBySeason(List<PlayerStatReadRow> rows) {
        return rows.stream()
                .collect(Collectors.groupingBy(PlayerStatReadRow::seasonId))
                .values().stream()
                .map(this::computeSeasonStats)
                .sorted(Comparator.comparing(SeasonPlayerStatsDto::seasonName, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private SeasonPlayerStatsDto computeSeasonStats(List<PlayerStatReadRow> rows) {
        PlayerStatReadRow any = rows.getFirst();
        Set<String> matchIds = distinctMatches(rows);
        int wins = (int) rows.stream().filter(PlayerStatReadRow::matchWon).map(PlayerStatReadRow::matchId).distinct().count();
        int motm = (int) rows.stream().filter(PlayerStatReadRow::playerOfTheMatch).map(PlayerStatReadRow::matchId).distinct().count();
        List<PlayerStatReadRow> batted = rows.stream().filter(PlayerStatReadRow::batted).toList();
        List<PlayerStatReadRow> bowled = rows.stream().filter(PlayerStatReadRow::bowled).toList();
        int runs = sum(batted, PlayerStatReadRow::runsScored);
        int balls = sum(batted, PlayerStatReadRow::ballsFaced);
        int outs = (int) batted.stream().filter(PlayerStatReadRow::out).count();
        int wickets = sum(bowled, PlayerStatReadRow::wicketsTaken);
        int conceded = sum(bowled, PlayerStatReadRow::runsConceded);
        int ballsBowled = sum(bowled, PlayerStatReadRow::ballsBowled);

        return new SeasonPlayerStatsDto(
                any.seasonId(), any.seasonName(), matchIds.size(), wins,
                batted.size(), runs,
                round2(outs == 0 ? runs : (double) runs / outs),
                round2(balls == 0 ? 0 : runs * 100.0 / balls),
                batted.stream().mapToInt(PlayerStatReadRow::runsScored).max().orElse(0),
                (int) batted.stream().filter(row -> row.runsScored() >= 50 && row.runsScored() < 100).count(),
                (int) batted.stream().filter(row -> row.runsScored() >= 100).count(),
                bowled.size(), wickets,
                round2(ballsBowled == 0 ? 0 : conceded * 6.0 / ballsBowled),
                round2(wickets == 0 ? 0 : (double) conceded / wickets),
                sum(rows, PlayerStatReadRow::catchesTaken),
                sum(rows, PlayerStatReadRow::runOuts),
                sum(rows, PlayerStatReadRow::stumpings),
                motm
        );
    }

    private List<TeamStatsForPlayerDto> getByTeam(List<PlayerStatReadRow> rows) {
        return rows.stream()
                .collect(Collectors.groupingBy(PlayerStatReadRow::teamId))
                .values().stream()
                .map(this::computeTeamStats)
                .sorted(Comparator.comparing(TeamStatsForPlayerDto::matchesPlayed).reversed())
                .toList();
    }

    private TeamStatsForPlayerDto computeTeamStats(List<PlayerStatReadRow> rows) {
        PlayerStatReadRow any = rows.getFirst();
        Set<String> matchIds = distinctMatches(rows);
        int wins = (int) rows.stream().filter(PlayerStatReadRow::matchWon).map(PlayerStatReadRow::matchId).distinct().count();
        int motm = (int) rows.stream().filter(PlayerStatReadRow::playerOfTheMatch).map(PlayerStatReadRow::matchId).distinct().count();
        List<PlayerStatReadRow> batted = rows.stream().filter(PlayerStatReadRow::batted).toList();
        List<PlayerStatReadRow> bowled = rows.stream().filter(PlayerStatReadRow::bowled).toList();
        int runs = sum(batted, PlayerStatReadRow::runsScored);
        int balls = sum(batted, PlayerStatReadRow::ballsFaced);
        int outs = (int) batted.stream().filter(PlayerStatReadRow::out).count();
        int wickets = sum(bowled, PlayerStatReadRow::wicketsTaken);
        int conceded = sum(bowled, PlayerStatReadRow::runsConceded);
        int ballsBowled = sum(bowled, PlayerStatReadRow::ballsBowled);

        return new TeamStatsForPlayerDto(
                any.teamId(), any.teamName(), matchIds.size(), wins,
                round2(matchIds.isEmpty() ? 0 : wins * 100.0 / matchIds.size()),
                batted.size(), runs,
                round2(outs == 0 ? runs : (double) runs / outs),
                round2(balls == 0 ? 0 : runs * 100.0 / balls),
                batted.stream().mapToInt(PlayerStatReadRow::runsScored).max().orElse(0),
                bowled.size(), wickets,
                round2(ballsBowled == 0 ? 0 : conceded * 6.0 / ballsBowled),
                round2(wickets == 0 ? 0 : (double) conceded / wickets),
                sum(rows, PlayerStatReadRow::catchesTaken),
                sum(rows, PlayerStatReadRow::runOuts),
                sum(rows, PlayerStatReadRow::stumpings),
                motm
        );
    }

    private BattingStatsResponse computeBatting(String playerId, String playerName, List<PlayerStatReadRow> rows) {
        List<PlayerStatReadRow> batted = rows.stream().filter(PlayerStatReadRow::batted).toList();
        int runs = sum(batted, PlayerStatReadRow::runsScored);
        int balls = sum(batted, PlayerStatReadRow::ballsFaced);
        int notOuts = (int) batted.stream().filter(row -> !row.out()).count();
        int outs = batted.size() - notOuts;
        return new BattingStatsResponse(
                playerId, playerName, runs, balls,
                round2(balls == 0 ? 0 : runs * 100.0 / balls),
                sum(batted, PlayerStatReadRow::foursHit),
                sum(batted, PlayerStatReadRow::sixesHit),
                notOuts,
                round2(outs == 0 ? runs : (double) runs / outs),
                batted.stream().mapToInt(PlayerStatReadRow::runsScored).max().orElse(0),
                (int) batted.stream().filter(row -> row.out() && row.runsScored() == 0).count(),
                distinctMatches(rows).size(),
                batted.size(),
                sum(batted, PlayerStatReadRow::dotBallsPlayed)
        );
    }

    private BowlingStatsResponse computeBowling(String playerId, String playerName, List<PlayerStatReadRow> rows) {
        List<PlayerStatReadRow> bowled = rows.stream().filter(PlayerStatReadRow::bowled).toList();
        int wickets = sum(bowled, PlayerStatReadRow::wicketsTaken);
        int conceded = sum(bowled, PlayerStatReadRow::runsConceded);
        int balls = sum(bowled, PlayerStatReadRow::ballsBowled);
        Map<String, Integer> byMatch = bowled.stream().collect(Collectors.groupingBy(
                PlayerStatReadRow::matchId, Collectors.summingInt(PlayerStatReadRow::wicketsTaken)));
        BestBowlingFigures best = bowled.stream()
                .max(Comparator.comparingInt(PlayerStatReadRow::wicketsTaken)
                        .thenComparing(Comparator.comparingInt(PlayerStatReadRow::runsConceded).reversed()))
                .map(row -> new BestBowlingFigures(row.wicketsTaken(), row.runsConceded(), row.ballsBowled()))
                .orElse(null);

        return new BowlingStatsResponse(
                playerId, playerName, wickets, conceded,
                round2(balls == 0 ? 0 : conceded * 6.0 / balls),
                toCricketOvers(balls),
                sum(bowled, PlayerStatReadRow::maidensBowled),
                wickets == 0 ? null : round2((double) conceded / wickets),
                best,
                (int) bowled.stream().filter(row -> row.wicketsTaken() >= 5).count(),
                (int) byMatch.values().stream().filter(value -> value >= 10).count(),
                distinctMatches(rows).size(),
                bowled.size(),
                sum(bowled, PlayerStatReadRow::dotBallsBowled)
        );
    }

    private FieldingAndMiscStatsResponse computeFielding(String playerId, String playerName, List<PlayerStatReadRow> rows) {
        return new FieldingAndMiscStatsResponse(
                playerId, playerName,
                sum(rows, PlayerStatReadRow::catchesTaken),
                sum(rows, PlayerStatReadRow::runOuts),
                sum(rows, PlayerStatReadRow::stumpings),
                distinctMatches(rows).size(),
                (int) rows.stream().filter(PlayerStatReadRow::playerOfTheMatch).map(PlayerStatReadRow::matchId).distinct().count()
        );
    }

    private Set<String> distinctMatches(List<PlayerStatReadRow> rows) {
        return rows.stream().map(PlayerStatReadRow::matchId).collect(Collectors.toSet());
    }

    private int sum(List<PlayerStatReadRow> rows, ToIntFunction<PlayerStatReadRow> mapper) {
        return rows.stream().mapToInt(mapper).sum();
    }

    private static double toCricketOvers(int balls) {
        return (balls / 6) + ((balls % 6) / 10.0);
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
