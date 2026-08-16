package com.gullycricket.backend.stats.service;

import com.gullycricket.backend.matches.entity.Match;
import com.gullycricket.backend.players.entity.Player;
import com.gullycricket.backend.players.entity.PlayerMatch;
import com.gullycricket.backend.players.repository.PlayerMatchRepository;
import com.gullycricket.backend.players.repository.PlayerRepository;
import com.gullycricket.backend.seasons.entity.Season;
import com.gullycricket.backend.stats.DTOs.*;
import com.gullycricket.backend.stats.enums.BattingSortBy;
import com.gullycricket.backend.stats.enums.BowlingSortBy;
import com.gullycricket.backend.stats.enums.FieldingSortBy;
import com.gullycricket.backend.stats.enums.MatchResult;
import com.gullycricket.backend.stats.specification.PlayerMatchSpecifications;
import com.gullycricket.backend.teams.entity.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayerStatsService {

    private final PlayerMatchRepository playerMatchRepository;
    private final PlayerRepository playerRepository;

    // =====================================================================
    // Leaderboards
    // =====================================================================

    public List<BattingStatsResponse> getBattingLeaderboard(BattingStatsFilter filter, BattingSortBy sortBy, Integer minInnings, Integer limit) {
        Specification<PlayerMatch> spec = PlayerMatchSpecifications
                .withCommonFilters(filter.seasonId(), filter.matchType(), filter.teamId(), filter.opponentTeamId(), filter.inningsNumber(), filter.result())
                .and(PlayerMatchSpecifications.battingPosition(filter.battingPosition()));

        Map<String, List<PlayerMatch>> byPlayer = groupByPlayer(playerMatchRepository.findAll(spec));

        List<BattingStatsResponse> results = byPlayer.values().stream()
                .map(rows -> computeBatting(playerId(rows), playerName(rows), rows))
                .filter(r -> minInnings == null || r.inningsPlayed() >= minInnings)
                .collect(Collectors.toList());

        results.sort(battingComparator(sortBy));
        return applyLimit(results, limit);
    }

    public List<BowlingStatsResponse> getBowlingLeaderboard(BowlingStatsFilter filter, BowlingSortBy sortBy, Integer minInnings, Integer limit) {
        Specification<PlayerMatch> spec = PlayerMatchSpecifications
                .withCommonFilters(filter.seasonId(), filter.matchType(), filter.teamId(), filter.opponentTeamId(), filter.inningsNumber(), filter.result());

        Map<String, List<PlayerMatch>> byPlayer = groupByPlayer(playerMatchRepository.findAll(spec));

        List<BowlingStatsResponse> results = byPlayer.values().stream()
                .map(rows -> computeBowling(playerId(rows), playerName(rows), rows))
                .filter(r -> minInnings == null || r.inningsBowled() >= minInnings)
                .collect(Collectors.toList());

        results.sort(bowlingComparator(sortBy));
        return applyLimit(results, limit);
    }

    public List<FieldingAndMiscStatsResponse> getFieldingLeaderboard(FieldingAndMiscStatsFilter filter, FieldingSortBy sortBy, Integer limit) {
        Specification<PlayerMatch> spec = PlayerMatchSpecifications
                .withCommonFilters(filter.seasonId(), filter.matchType(), filter.teamId(), filter.opponentTeamId(), filter.inningsNumber(), filter.result());

        Map<String, List<PlayerMatch>> byPlayer = groupByPlayer(playerMatchRepository.findAll(spec));

        List<FieldingAndMiscStatsResponse> results = byPlayer.values().stream()
                .map(rows -> computeFielding(playerId(rows), playerName(rows), rows))
                .collect(Collectors.toList());

        results.sort(fieldingComparator(sortBy));
        return applyLimit(results, limit);
    }

    // =====================================================================
    // Player Profile
    // =====================================================================

    public PlayerProfileDto getPlayerProfile(String playerId, String seasonId) {
        List<PlayerMatch> rows = seasonId != null
                ? playerMatchRepository.findByPlayer_IdAndSeason_Id(playerId, seasonId)
                : playerMatchRepository.findByPlayer_Id(playerId);

        Player player = rows.isEmpty()
                ? playerRepository.findById(playerId).orElse(null)
                : rows.get(0).getPlayer();

        String name = player != null ? player.getName() : null;

        Set<String> distinctMatchIds = rows.stream().map(pm -> pm.getMatch().getId()).collect(Collectors.toSet());
        Set<String> wonMatchIds = rows.stream().filter(PlayerMatch::isMatchWon).map(pm -> pm.getMatch().getId()).collect(Collectors.toSet());
        Set<String> motmMatchIds = rows.stream().filter(PlayerMatch::isPlayerOfTheMatch).map(pm -> pm.getMatch().getId()).collect(Collectors.toSet());

        int matchesPlayed = distinctMatchIds.size();
        int matchesWon = wonMatchIds.size();

        BattingStatsResponse batting = computeBatting(playerId, name, rows);
        BowlingStatsResponse bowling = computeBowling(playerId, name, rows);
        FieldingAndMiscStatsResponse fielding = computeFielding(playerId, name, rows);

        return new PlayerProfileDto(
                playerId,
                name,
                matchesPlayed,
                matchesWon,
                round2(matchesPlayed == 0 ? 0 : matchesWon * 100.0 / matchesPlayed),
                motmMatchIds.size(),
                batting,
                bowling,
                fielding,
                getRecentForm(rows, 3),
                getByBattingPosition(rows),
                getByInnings(rows),
                getByMatchResult(rows),
                getBySeason(rows),
                getByTeam(rows)
        );
    }

    private List<RecentInningDto> getRecentForm(List<PlayerMatch> rows, int count) {
        List<PlayerMatch> batted = rows.stream().filter(PlayerMatch::isBatted).collect(Collectors.toList());

        batted.sort((a, b) -> {
            LocalDateTime da = a.getMatch().getCompletedAt();
            LocalDateTime db = b.getMatch().getCompletedAt();
            if (da == null && db == null) return 0;
            if (da == null) return 1;
            if (db == null) return -1;
            return db.compareTo(da);
        });

        return batted.stream()
                .limit(count)
                .map(pm -> new RecentInningDto(
                        pm.getMatch().getId(),
                        pm.getSeason().getId(),
                        pm.getSeason().getSeasonName(),
                        pm.getTeamRepresented().getId(),
                        pm.getTeamRepresented().getTeamName(),
                        pm.getOppositionTeam().getId(),
                        pm.getOppositionTeam().getTeamName(),
                        pm.getBattingPosition(),
                        pm.getRunsScored(),
                        pm.getBallsFaced(),
                        pm.getFoursHit(),
                        pm.getSixesHit(),
                        pm.isOut(),
                        pm.isMatchWon(),
                        pm.getMatch().getCompletedAt()
                ))
                .toList();
    }

    private List<BattingPositionStatsDto> getByBattingPosition(List<PlayerMatch> rows) {
        Map<Integer, List<PlayerMatch>> byPosition = rows.stream()
                .filter(PlayerMatch::isBatted)
                .filter(pm -> pm.getBattingPosition() != null)
                .collect(Collectors.groupingBy(PlayerMatch::getBattingPosition));

        return byPosition.entrySet().stream()
                .map(e -> computePositionStats(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(BattingPositionStatsDto::battingPosition))
                .toList();
    }

    private BattingPositionStatsDto computePositionStats(Integer position, List<PlayerMatch> rows) {
        int innings = rows.size();
        int notOuts = (int) rows.stream().filter(pm -> !pm.isOut()).count();
        int outs = innings - notOuts;
        int runs = sumInt(rows, PlayerMatch::getRunsScored);
        int balls = sumInt(rows, PlayerMatch::getBallsFaced);
        int fours = sumInt(rows, PlayerMatch::getFoursHit);
        int sixes = sumInt(rows, PlayerMatch::getSixesHit);
        int highest = rows.stream().mapToInt(PlayerMatch::getRunsScored).max().orElse(0);
        double avg = outs == 0 ? runs : (double) runs / outs;
        double sr = balls == 0 ? 0 : runs * 100.0 / balls;

        return new BattingPositionStatsDto(position, innings, notOuts, runs, balls, round2(avg), round2(sr), highest, fours, sixes);
    }

    private List<PlayerSplitStatsDto> getByInnings(List<PlayerMatch> rows) {
        Map<Integer, List<PlayerMatch>> byInnings = rows.stream().collect(Collectors.groupingBy(PlayerMatch::getInningsNumber));

        return byInnings.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> computeSplit("innings_" + e.getKey(), "Innings " + e.getKey(), e.getValue()))
                .toList();
    }

    private List<PlayerSplitStatsDto> getByMatchResult(List<PlayerMatch> rows) {
        Map<MatchResult, List<PlayerMatch>> byResult = rows.stream().collect(Collectors.groupingBy(this::resultOf));

        return Arrays.stream(MatchResult.values())
                .filter(byResult::containsKey)
                .map(result -> computeSplit(result.name(), toLabel(result), byResult.get(result)))
                .toList();
    }

    private String toLabel(MatchResult result) {
        return switch (result) {
            case WIN -> "Won";
            case LOSS -> "Lost";
            case TIE -> "Tied";
            case NO_RESULT -> "No Result";
        };
    }

    private MatchResult resultOf(PlayerMatch pm) {
        Match match = pm.getMatch();
        if (Boolean.TRUE.equals(match.getIsMatchTied())) return MatchResult.TIE;
        if (Boolean.TRUE.equals(match.getIsMatchDrawn())) return MatchResult.NO_RESULT;
        return pm.isMatchWon() ? MatchResult.WIN : MatchResult.LOSS;
    }

    private PlayerSplitStatsDto computeSplit(String key, String label, List<PlayerMatch> rows) {
        Set<String> matchIds = rows.stream().map(pm -> pm.getMatch().getId()).collect(Collectors.toSet());
        Set<String> wonMatchIds = rows.stream().filter(PlayerMatch::isMatchWon).map(pm -> pm.getMatch().getId()).collect(Collectors.toSet());

        List<PlayerMatch> batted = rows.stream().filter(PlayerMatch::isBatted).toList();
        List<PlayerMatch> bowled = rows.stream().filter(PlayerMatch::isBowled).toList();

        int runs = sumInt(batted, PlayerMatch::getRunsScored);
        int balls = sumInt(batted, PlayerMatch::getBallsFaced);
        int outs = (int) batted.stream().filter(PlayerMatch::isOut).count();
        int highest = batted.stream().mapToInt(PlayerMatch::getRunsScored).max().orElse(0);
        double battingAvg = outs == 0 ? runs : (double) runs / outs;
        double sr = balls == 0 ? 0 : runs * 100.0 / balls;

        int wickets = sumInt(bowled, PlayerMatch::getWicketsTaken);
        int runsConceded = sumInt(bowled, PlayerMatch::getRunsConceded);
        int ballsBowled = sumInt(bowled, PlayerMatch::getBallsBowled);
        double economy = ballsBowled == 0 ? 0 : runsConceded / (ballsBowled / 6.0);
        double bowlingAvg = wickets == 0 ? 0 : (double) runsConceded / wickets;

        return new PlayerSplitStatsDto(
                key, label,
                matchIds.size(), wonMatchIds.size(),
                batted.size(), runs, balls, round2(battingAvg), round2(sr), highest,
                bowled.size(), wickets, runsConceded, round2(economy), round2(bowlingAvg)
        );
    }

    private List<SeasonPlayerStatsDto> getBySeason(List<PlayerMatch> rows) {
        Map<String, List<PlayerMatch>> bySeason = rows.stream().collect(Collectors.groupingBy(pm -> pm.getSeason().getId()));

        return bySeason.values().stream()
                .map(list -> computeSeasonStats(list.get(0).getSeason(), list))
                .sorted(Comparator.comparing(SeasonPlayerStatsDto::seasonName, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private SeasonPlayerStatsDto computeSeasonStats(Season season, List<PlayerMatch> rows) {
        Set<String> matchIds = rows.stream().map(pm -> pm.getMatch().getId()).collect(Collectors.toSet());
        Set<String> wonMatchIds = rows.stream().filter(PlayerMatch::isMatchWon).map(pm -> pm.getMatch().getId()).collect(Collectors.toSet());
        Set<String> motmMatchIds = rows.stream().filter(PlayerMatch::isPlayerOfTheMatch).map(pm -> pm.getMatch().getId()).collect(Collectors.toSet());

        List<PlayerMatch> batted = rows.stream().filter(PlayerMatch::isBatted).toList();
        List<PlayerMatch> bowled = rows.stream().filter(PlayerMatch::isBowled).toList();

        int runs = sumInt(batted, PlayerMatch::getRunsScored);
        int balls = sumInt(batted, PlayerMatch::getBallsFaced);
        int outs = (int) batted.stream().filter(PlayerMatch::isOut).count();
        int highest = batted.stream().mapToInt(PlayerMatch::getRunsScored).max().orElse(0);
        double avg = outs == 0 ? runs : (double) runs / outs;
        double sr = balls == 0 ? 0 : runs * 100.0 / balls;
        int fifties = (int) batted.stream().filter(pm -> pm.getRunsScored() >= 50 && pm.getRunsScored() < 100).count();
        int hundreds = (int) batted.stream().filter(pm -> pm.getRunsScored() >= 100).count();

        int wickets = sumInt(bowled, PlayerMatch::getWicketsTaken);
        int runsConceded = sumInt(bowled, PlayerMatch::getRunsConceded);
        int ballsBowled = sumInt(bowled, PlayerMatch::getBallsBowled);
        double economy = ballsBowled == 0 ? 0 : runsConceded / (ballsBowled / 6.0);
        double bowlingAvg = wickets == 0 ? 0 : (double) runsConceded / wickets;

        int catches = sumInt(rows, PlayerMatch::getCatchesTaken);
        int runOuts = sumInt(rows, PlayerMatch::getRunOuts);
        int stumpings = sumInt(rows, PlayerMatch::getStumpings);

        return new SeasonPlayerStatsDto(
                season.getId(), season.getSeasonName(),
                matchIds.size(), wonMatchIds.size(),
                batted.size(), runs, round2(avg), round2(sr), highest, fifties, hundreds,
                bowled.size(), wickets, round2(economy), round2(bowlingAvg),
                catches, runOuts, stumpings, motmMatchIds.size()
        );
    }

    private List<TeamStatsForPlayerDto> getByTeam(List<PlayerMatch> rows) {
        Map<String, List<PlayerMatch>> byTeam = rows.stream().collect(Collectors.groupingBy(pm -> pm.getTeamRepresented().getId()));

        return byTeam.values().stream()
                .map(list -> computeTeamStatsForPlayer(list.get(0).getTeamRepresented(), list))
                .sorted(Comparator.comparing(TeamStatsForPlayerDto::matchesPlayed).reversed())
                .toList();
    }

    private TeamStatsForPlayerDto computeTeamStatsForPlayer(Team team, List<PlayerMatch> rows) {
        Set<String> matchIds = rows.stream().map(pm -> pm.getMatch().getId()).collect(Collectors.toSet());
        Set<String> wonMatchIds = rows.stream().filter(PlayerMatch::isMatchWon).map(pm -> pm.getMatch().getId()).collect(Collectors.toSet());

        List<PlayerMatch> batted = rows.stream().filter(PlayerMatch::isBatted).toList();
        List<PlayerMatch> bowled = rows.stream().filter(PlayerMatch::isBowled).toList();

        int runs = sumInt(batted, PlayerMatch::getRunsScored);
        int balls = sumInt(batted, PlayerMatch::getBallsFaced);
        int outs = (int) batted.stream().filter(PlayerMatch::isOut).count();
        int highest = batted.stream().mapToInt(PlayerMatch::getRunsScored).max().orElse(0);
        double avg = outs == 0 ? runs : (double) runs / outs;
        double sr = balls == 0 ? 0 : runs * 100.0 / balls;

        int wickets = sumInt(bowled, PlayerMatch::getWicketsTaken);
        int runsConceded = sumInt(bowled, PlayerMatch::getRunsConceded);
        int ballsBowled = sumInt(bowled, PlayerMatch::getBallsBowled);
        double economy = ballsBowled == 0 ? 0 : runsConceded / (ballsBowled / 6.0);
        double bowlingAvg = wickets == 0 ? 0 : (double) runsConceded / wickets;

        int catches = sumInt(rows, PlayerMatch::getCatchesTaken);
        int runOuts = sumInt(rows, PlayerMatch::getRunOuts);
        int stumpings = sumInt(rows, PlayerMatch::getStumpings);
        int motm = (int) rows.stream().filter(PlayerMatch::isPlayerOfTheMatch).map(pm -> pm.getMatch().getId()).distinct().count();

        return new TeamStatsForPlayerDto(
                team.getId(), team.getTeamName(),
                matchIds.size(), wonMatchIds.size(),
                round2(matchIds.isEmpty() ? 0 : wonMatchIds.size() * 100.0 / matchIds.size()),
                batted.size(), runs, round2(avg), round2(sr), highest,
                bowled.size(), wickets, round2(economy), round2(bowlingAvg),
                catches, runOuts, stumpings, motm
        );
    }

    // =====================================================================
    // Shared aggregate computations
    // =====================================================================

    private BattingStatsResponse computeBatting(String playerId, String playerName, List<PlayerMatch> rows) {
        List<PlayerMatch> batted = rows.stream().filter(PlayerMatch::isBatted).toList();

        int totalRuns = sumInt(batted, PlayerMatch::getRunsScored);
        int totalBalls = sumInt(batted, PlayerMatch::getBallsFaced);
        int fours = sumInt(batted, PlayerMatch::getFoursHit);
        int sixes = sumInt(batted, PlayerMatch::getSixesHit);
        int dotBalls = sumInt(batted, PlayerMatch::getDotBallsPlayed);
        int notOuts = (int) batted.stream().filter(pm -> !pm.isOut()).count();
        int innings = batted.size();
        int outs = innings - notOuts;
        double average = outs == 0 ? totalRuns : (double) totalRuns / outs;
        double strikeRate = totalBalls == 0 ? 0 : totalRuns * 100.0 / totalBalls;
        int highest = batted.stream().mapToInt(PlayerMatch::getRunsScored).max().orElse(0);
        int ducks = (int) batted.stream().filter(pm -> pm.isOut() && pm.getRunsScored() == 0).count();
        int matches = (int) rows.stream().map(pm -> pm.getMatch().getId()).distinct().count();

        return new BattingStatsResponse(
                playerId, playerName, totalRuns, totalBalls, round2(strikeRate), fours, sixes,
                notOuts, round2(average), highest, ducks, matches, innings, dotBalls
        );
    }

    private BowlingStatsResponse computeBowling(String playerId, String playerName, List<PlayerMatch> rows) {
        List<PlayerMatch> bowled = rows.stream().filter(PlayerMatch::isBowled).toList();

        int wickets = sumInt(bowled, PlayerMatch::getWicketsTaken);
        int runsConceded = sumInt(bowled, PlayerMatch::getRunsConceded);
        int ballsBowled = sumInt(bowled, PlayerMatch::getBallsBowled);
        int maidens = sumInt(bowled, PlayerMatch::getMaidensBowled);
        int dotBalls = sumInt(bowled, PlayerMatch::getDotBallsBowled);
        double economy = ballsBowled == 0 ? 0 : runsConceded / (ballsBowled / 6.0);
        double average = wickets == 0 ? 0 : (double) runsConceded / wickets;
        int fiveWicketHauls = (int) bowled.stream().filter(pm -> pm.getWicketsTaken() >= 5).count();
        int tenWicketHauls = (int) bowled.stream().filter(pm -> pm.getWicketsTaken() >= 10).count();
        int matches = (int) rows.stream().map(pm -> pm.getMatch().getId()).distinct().count();

        return new BowlingStatsResponse(
                playerId, playerName, wickets, runsConceded, round2(economy), ballsBowled / 6,
                maidens, round2(average), null, fiveWicketHauls, tenWicketHauls, matches, bowled.size(), dotBalls
        );
    }

    private FieldingAndMiscStatsResponse computeFielding(String playerId, String playerName, List<PlayerMatch> rows) {
        int catches = sumInt(rows, PlayerMatch::getCatchesTaken);
        int runOuts = sumInt(rows, PlayerMatch::getRunOuts);
        int stumpings = sumInt(rows, PlayerMatch::getStumpings);
        int matches = (int) rows.stream().map(pm -> pm.getMatch().getId()).distinct().count();
        int motm = (int) rows.stream().filter(PlayerMatch::isPlayerOfTheMatch).map(pm -> pm.getMatch().getId()).distinct().count();

        return new FieldingAndMiscStatsResponse(playerId, playerName, catches, runOuts, stumpings, matches, motm);
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private Map<String, List<PlayerMatch>> groupByPlayer(List<PlayerMatch> rows) {
        return rows.stream().collect(Collectors.groupingBy(pm -> pm.getPlayer().getId()));
    }

    private String playerId(List<PlayerMatch> rows) {
        return rows.get(0).getPlayer().getId();
    }

    private String playerName(List<PlayerMatch> rows) {
        return rows.get(0).getPlayer().getName();
    }

    private int sumInt(List<PlayerMatch> rows, java.util.function.ToIntFunction<PlayerMatch> mapper) {
        return rows.stream().mapToInt(mapper).sum();
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private <T> List<T> applyLimit(List<T> list, Integer limit) {
        return limit != null ? list.stream().limit(limit).toList() : list;
    }

    private Comparator<BattingStatsResponse> battingComparator(BattingSortBy sortBy) {
        BattingSortBy sort = sortBy != null ? sortBy : BattingSortBy.RUNS;
        Comparator<BattingStatsResponse> comparator = switch (sort) {
            case AVERAGE -> Comparator.comparing(BattingStatsResponse::average);
            case STRIKE_RATE -> Comparator.comparing(BattingStatsResponse::strikeRate);
            case HIGHEST_SCORE -> Comparator.comparing(BattingStatsResponse::highestScore);
            case FOURS -> Comparator.comparing(BattingStatsResponse::totalFours);
            case SIXES -> Comparator.comparing(BattingStatsResponse::totalSixes);
            case MATCHES -> Comparator.comparing(BattingStatsResponse::totalMatchesPlayed);
            case RUNS -> Comparator.comparing(BattingStatsResponse::totalRuns);
        };
        return comparator.reversed();
    }

    private Comparator<BowlingStatsResponse> bowlingComparator(BowlingSortBy sortBy) {
        BowlingSortBy sort = sortBy != null ? sortBy : BowlingSortBy.WICKETS;
        return switch (sort) {
            case WICKETS -> Comparator.comparing(BowlingStatsResponse::totalWickets).reversed();
            case MATCHES -> Comparator.comparing(BowlingStatsResponse::totalMatchesPlayed).reversed();
            // Lower economy/average is better, so ascending order for these.
            case ECONOMY -> Comparator.comparing(BowlingStatsResponse::economyRate);
            case AVERAGE -> Comparator.comparing(BowlingStatsResponse::average);
        };
    }

    private Comparator<FieldingAndMiscStatsResponse> fieldingComparator(FieldingSortBy sortBy) {
        FieldingSortBy sort = sortBy != null ? sortBy : FieldingSortBy.DISMISSALS;
        return switch (sort) {
            case CATCHES -> Comparator.comparing(FieldingAndMiscStatsResponse::totalCatches).reversed();
            case RUN_OUTS -> Comparator.comparing(FieldingAndMiscStatsResponse::totalRunOuts).reversed();
            case STUMPINGS -> Comparator.comparing(FieldingAndMiscStatsResponse::totalStumpings).reversed();
            case MAN_OF_THE_MATCH -> Comparator.comparing(FieldingAndMiscStatsResponse::manOfTheMatchAwards).reversed();
            case DISMISSALS -> Comparator.comparing(
                    (FieldingAndMiscStatsResponse r) -> r.totalCatches() + r.totalRunOuts() + r.totalStumpings()
            ).reversed();
        };
    }
}
