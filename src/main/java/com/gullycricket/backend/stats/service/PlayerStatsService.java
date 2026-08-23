package com.gullycricket.backend.stats.service;

import com.gullycricket.backend.common.exception.ResourceNotFoundException;
import com.gullycricket.backend.config.CacheNames;
import com.gullycricket.backend.players.entity.Player;
import com.gullycricket.backend.players.repository.PlayerRepository;
import com.gullycricket.backend.stats.dto.*;
import com.gullycricket.backend.stats.enums.*;
import com.gullycricket.backend.stats.repository.PlayerProfileReadRepository;
import com.gullycricket.backend.stats.repository.PlayerStatReadRow;
import com.gullycricket.backend.stats.repository.StatsReadRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

@Service
public class PlayerStatsService {

    private final PlayerRepository playerRepository;
    private final StatsReadRepository statsReadRepository;
    private final PlayerProfileReadRepository playerProfileReadRepository;

    public PlayerStatsService(PlayerRepository playerRepository, StatsReadRepository statsReadRepository, PlayerProfileReadRepository playerProfileReadRepository) {
        this.playerRepository = playerRepository;
        this.statsReadRepository = statsReadRepository;
        this.playerProfileReadRepository = playerProfileReadRepository;
    }

    private static double toCricketOvers(int balls) {
        return (balls / 6) + ((balls % 6) / 10.0);
    }

    private static Double battingAverage(int runs, int outs) {
        return outs == 0 ? null : round2((double) runs / outs);
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    // Leaderboards are aggregated in PostgreSQL and never hydrate Match.matchData.
    @Cacheable(value = CacheNames.BATTING_LEADERBOARD, sync = true)
    public List<BattingStatsResponse> getBattingLeaderboard(BattingStatsFilter filter, BattingSortBy sortBy, Integer minInnings, Integer limit) {
        return statsReadRepository.findBattingLeaderboard(filter, sortBy, minInnings, limit);
    }

    @Cacheable(value = CacheNames.BOWLING_LEADERBOARD, sync = true)
    public List<BowlingStatsResponse> getBowlingLeaderboard(BowlingStatsFilter filter, BowlingSortBy sortBy, Integer minInnings, Integer limit) {
        return statsReadRepository.findBowlingLeaderboard(filter, sortBy, minInnings, limit);
    }

    @Cacheable(value = CacheNames.FIELDING_LEADERBOARD, sync = true)
    public List<FieldingAndMiscStatsResponse> getFieldingLeaderboard(FieldingAndMiscStatsFilter filter, FieldingSortBy sortBy, Integer limit) {
        return statsReadRepository.findFieldingLeaderboard(filter, sortBy, limit);
    }

    public List<PartnershipInningsDto> getPartnershipInnings(PartnershipStatsFilter filter, Integer limit) {
        return statsReadRepository.findPartnershipInnings(filter, limit);
    }

    /** Backward-compatible service alias for existing callers. */
    public List<PartnershipInningsDto> getPartnershipStats(PartnershipStatsFilter filter, Integer limit) {
        return getPartnershipInnings(filter, limit);
    }

    public List<PartnershipStatsResponse> getPartnershipAggregatedStats(PartnershipStatsFilter filter, Integer limit) {
        return statsReadRepository.findPartnershipAggregatedStats(filter, limit);
    }

    public List<PartnershipInningsDto> getPartnershipHistory(String playerId, String partnerId, String seasonId, Integer limit) {
        return statsReadRepository.findPartnershipHistory(playerId, partnerId, seasonId, limit);
    }

    public List<RivalryStatsResponse> getRivalryStats(RivalryStatsFilter filter, Integer limit) {
        return statsReadRepository.findRivalryStats(filter, limit);
    }

    public List<RivalryInningsDto> getRivalryInnings(RivalryStatsFilter filter, Integer limit) {
        return statsReadRepository.findRivalryInnings(filter, limit);
    }

    public PlayerVsPlayerDto getPlayerVsPlayer(String player1Id, String player2Id, String seasonId) {
        Player player1 = playerRepository.findById(player1Id)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + player1Id));
        Player player2 = playerRepository.findById(player2Id)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + player2Id));

        RivalryStatsResponse player1Batting = firstOrNull(statsReadRepository.findRivalryStats(
                new RivalryStatsFilter(seasonId, null, null, null, null, null, player1Id, player2Id, null, null, null), 1));
        RivalryStatsResponse player2Batting = firstOrNull(statsReadRepository.findRivalryStats(
                new RivalryStatsFilter(seasonId, null, null, null, null, null, player2Id, player1Id, null, null, null), 1));
        PartnershipStatsResponse partnership = firstOrNull(statsReadRepository.findPartnershipAggregatedStats(
                new PartnershipStatsFilter(seasonId, null, null, null, null, null, null, player1Id, player2Id, null), 1));

        return new PlayerVsPlayerDto(
                seasonId, player1Id, player1.getName(), player2Id, player2.getName(),
                player1Batting, player2Batting, partnership
        );
    }

    private static <T> T firstOrNull(List<T> values) {
        return values == null || values.isEmpty() ? null : values.getFirst();
    }

    public PlayerComparisonDto comparePlayers(String player1Id, String player2Id, String seasonId) {
        PlayerProfileDto first = getPlayerProfile(player1Id, seasonId);
        PlayerProfileDto second = getPlayerProfile(player2Id, seasonId);
        return new PlayerComparisonDto(
                seasonId,
                comparisonSide(first),
                comparisonSide(second)
        );
    }

    private PlayerComparisonSideDto comparisonSide(PlayerProfileDto profile) {
        return new PlayerComparisonSideDto(
                profile.playerId(), profile.playerName(), profile.totalMatchesPlayed(), profile.totalMatchesWon(),
                profile.winPercentage(), profile.playerOfTheMatchAwards(), profile.overallBatting(),
                profile.overallBowling(), profile.overallFielding()
        );
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

        PlayerParticipationSummaryDto participation = statsReadRepository.findPlayerParticipationSummary(playerId, seasonId);
        int matchesPlayed = participation == null ? distinctMatches(rows).size() : participation.matchesPlayed();
        int matchesWon = participation == null
                ? (int) rows.stream().filter(PlayerStatReadRow::matchWon).map(PlayerStatReadRow::matchId).distinct().count()
                : participation.matchesWon();
        int motm = participation == null
                ? (int) rows.stream().filter(PlayerStatReadRow::playerOfTheMatch).map(PlayerStatReadRow::matchId).distinct().count()
                : participation.playerOfTheMatchAwards();

        return new PlayerProfileDto(
                playerId,
                name,
                matchesPlayed,
                matchesWon,
                round2(matchesPlayed == 0 ? 0 : matchesWon * 100.0 / matchesPlayed),
                motm,
                computeBatting(playerId, name, rows, matchesPlayed),
                computeBowling(playerId, name, rows, matchesPlayed),
                computeFielding(playerId, name, rows, matchesPlayed),
                getRecentPerformances(rows, 5),
                getByBattingPosition(rows),
                getByInnings(rows),
                getByMatchResult(rows),
                getBySeason(rows),
                getByTeam(rows)
        );
    }

    private List<RecentPerformanceDto> getRecentPerformances(List<PlayerStatReadRow> rows, int count) {
        List<PlayerStatReadRow> sorted = rows.stream()
                .sorted(Comparator.comparing(PlayerStatReadRow::completedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        Map<String, List<PlayerStatReadRow>> byMatch = new LinkedHashMap<>();
        for (PlayerStatReadRow row : sorted) {
            byMatch.computeIfAbsent(row.matchId(), ignored -> new ArrayList<>()).add(row);
        }

        return byMatch.values().stream()
                .limit(count)
                .map(matchRows -> {
                    PlayerStatReadRow any = matchRows.getFirst();
                    List<RecentBattingPerformanceDto> batting = matchRows.stream()
                            .filter(PlayerStatReadRow::batted)
                            .sorted(Comparator.comparing(PlayerStatReadRow::inningsNumber,
                                    Comparator.nullsLast(Comparator.naturalOrder())))
                            .map(row -> new RecentBattingPerformanceDto(
                                    row.inningsNumber(), row.battingPosition(), row.runsScored(), row.ballsFaced(),
                                    row.foursHit(), row.sixesHit(), row.out()))
                            .toList();
                    List<RecentBowlingPerformanceDto> bowling = matchRows.stream()
                            .filter(PlayerStatReadRow::bowled)
                            .sorted(Comparator.comparing(PlayerStatReadRow::inningsNumber,
                                    Comparator.nullsLast(Comparator.naturalOrder())))
                            .map(row -> new RecentBowlingPerformanceDto(
                                    row.inningsNumber(), row.wicketsTaken(), row.runsConceded(), row.ballsBowled(),
                                    toCricketOvers(row.ballsBowled()), row.maidensBowled(), row.dotBallsBowled()))
                            .toList();

                    return new RecentPerformanceDto(
                            any.matchId(), any.seasonId(), any.seasonName(), any.teamId(), any.teamName(),
                            any.opponentTeamId(), any.opponentTeamName(), any.matchWon(), resultOf(any), any.completedAt(),
                            batting, bowling,
                            sum(matchRows, PlayerStatReadRow::catchesTaken),
                            sum(matchRows, PlayerStatReadRow::runOuts),
                            sum(matchRows, PlayerStatReadRow::stumpings)
                    );
                })
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
                battingAverage(runs, outs),
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
                battingAverage(runs, outs),
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
                battingAverage(runs, outs),
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
                battingAverage(runs, outs),
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

    private BattingStatsResponse computeBatting(String playerId, String playerName, List<PlayerStatReadRow> rows, int totalMatchesPlayed) {
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
                battingAverage(runs, outs),
                batted.stream().mapToInt(PlayerStatReadRow::runsScored).max().orElse(0),
                (int) batted.stream().filter(row -> row.out() && row.runsScored() == 0).count(),
                totalMatchesPlayed,
                batted.size(),
                sum(batted, PlayerStatReadRow::dotBallsPlayed)
        );
    }

    private BowlingStatsResponse computeBowling(String playerId, String playerName, List<PlayerStatReadRow> rows, int totalMatchesPlayed) {
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
                totalMatchesPlayed,
                bowled.size(),
                sum(bowled, PlayerStatReadRow::dotBallsBowled)
        );
    }

    private FieldingAndMiscStatsResponse computeFielding(String playerId, String playerName, List<PlayerStatReadRow> rows, int totalMatchesPlayed) {
        return new FieldingAndMiscStatsResponse(
                playerId, playerName,
                sum(rows, PlayerStatReadRow::catchesTaken),
                sum(rows, PlayerStatReadRow::runOuts),
                sum(rows, PlayerStatReadRow::stumpings),
                totalMatchesPlayed,
                (int) rows.stream().filter(PlayerStatReadRow::playerOfTheMatch).map(PlayerStatReadRow::matchId).distinct().count()
        );
    }

    private Set<String> distinctMatches(List<PlayerStatReadRow> rows) {
        return rows.stream().map(PlayerStatReadRow::matchId).collect(Collectors.toSet());
    }

    private int sum(List<PlayerStatReadRow> rows, ToIntFunction<PlayerStatReadRow> mapper) {
        return rows.stream().mapToInt(mapper).sum();
    }
}
