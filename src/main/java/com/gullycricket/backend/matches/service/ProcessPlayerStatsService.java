package com.gullycricket.backend.matches.service;

import com.gullycricket.backend.common.exception.BadRequestException;
import com.gullycricket.backend.common.util.NameNormalizer;
import com.gullycricket.backend.matches.dto.*;
import com.gullycricket.backend.matches.entity.Match;
import com.gullycricket.backend.players.entity.*;
import com.gullycricket.backend.players.repository.PlayerMatchRepository;
import com.gullycricket.backend.players.repository.MatchPlayerParticipationRepository;
import com.gullycricket.backend.players.repository.PlayerPartnershipsRepository;
import com.gullycricket.backend.players.repository.PlayerRivalryRepository;
import com.gullycricket.backend.players.service.PlayerService;
import com.gullycricket.backend.seasons.entity.Season;
import com.gullycricket.backend.seasons.entity.SeasonPlayer;
import com.gullycricket.backend.seasons.repository.SeasonPlayerRepository;
import com.gullycricket.backend.teams.entity.PlayerTeam;
import com.gullycricket.backend.teams.entity.Team;
import com.gullycricket.backend.teams.service.PlayerTeamService;
import com.gullycricket.backend.teams.service.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProcessPlayerStatsService {

    private final TeamService teamService;
    private final PlayerService playerService;
    private final PlayerTeamService playerTeamService;
    private final SeasonPlayerRepository seasonPlayerRepository;
    private final PlayerMatchRepository playerMatchRepository;
    private final MatchPlayerParticipationRepository matchPlayerParticipationRepository;
    private final PlayerPartnershipsRepository playerPartnershipsRepository;
    private final PlayerRivalryRepository playerRivalryRepository;

    public void processPlayerStats(Match match, MatchDataDto matchData) {
        // MatchService already loaded the managed Season for this transaction. Reusing it
        // avoids another remote DB round trip during match completion.
        Season season = match.getSeason();
        ProcessingContext ctx = createContext(match, matchData, season);

        processTeams(ctx, matchData.teams());
        processMatchParticipation(ctx, matchData.teams());
        processBattingStats(ctx, matchData.innings());
        processBowlingStats(ctx, matchData.innings());
        processFieldingStats(ctx, matchData.innings());
        processPartnershipsAndRivalries(ctx, matchData.innings());
        saveAllProcessedStats(ctx);
    }

    private ProcessingContext createContext(Match match, MatchDataDto matchData, Season season) {
        String playerOfTheMatch = matchData.result() != null ? matchData.result().manOfTheMatch() : null;

        List<String> allPlayerNames = new ArrayList<>();
        allPlayerNames.addAll(matchData.teams().get("teamA").players());
        allPlayerNames.addAll(matchData.teams().get("teamB").players());

        Map<String, Player> playerMap = playerService.getPlayersByNameIn(allPlayerNames).stream()
                .collect(Collectors.toMap(Player::getName, p -> p));
        Set<String> seasonPlayerIds = seasonPlayerRepository.findPlayerIdsBySeasonId(season.getId());

        return new ProcessingContext(
                match,
                season,
                matchData.matchType(),
                playerOfTheMatch,
                matchData.rules(),
                playerMap,
                new HashMap<>(),
                seasonPlayerIds,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new ArrayList<>()
        );
    }

    private void processTeams(ProcessingContext ctx, Map<String, TeamDto> teams) {
        processSingleTeam(ctx, teams.get("teamA"));
        processSingleTeam(ctx, teams.get("teamB"));
    }

    private void processSingleTeam(ProcessingContext ctx, TeamDto teamDto) {
        String teamName = canonical(teamDto.name());

        // MatchService has already resolved and persisted both teams. Reuse those
        // managed entities instead of performing another find + save for each team.
        Team team = matchTeam(ctx.match(), teamName);
        if (team == null) {
            team = teamService.getTeamByName(teamName);
            if (team == null) {
                team = new Team();
                team.setTeamName(teamName);
                team.getSeasonsPlayed().add(ctx.season());
                team = teamService.saveTeam(team);
            }
        }
        ctx.teamMap().put(teamName, team);

        Set<String> existingPlayerNames = playerTeamService.findExistingPlayerNamesByTeamAndSeason(team, ctx.season());

        for (String rawPlayerName : teamDto.players()) {
            String playerName = canonical(rawPlayerName);
            Player player = ctx.playerMap().get(playerName);
            if (player == null) {
                player = new Player();
                player.setName(playerName);
                ctx.playerMap().put(playerName, player);
            }

            if (!existingPlayerNames.contains(playerName)) {
                PlayerTeam playerTeam = new PlayerTeam();
                playerTeam.setPlayer(player);
                playerTeam.setTeam(team);
                playerTeam.setSeason(ctx.season());
                playerTeam.setActive(true);
                ctx.playerTeams().add(playerTeam);
            }
        }
    }

    private void processMatchParticipation(ProcessingContext ctx, Map<String, TeamDto> teams) {
        TeamDto teamA = teams.get("teamA");
        TeamDto teamB = teams.get("teamB");
        if (teamA == null || teamB == null) {
            throw new BadRequestException("Both match teams are required to build participation stats");
        }
        addParticipationForTeam(ctx, teamA, teamB);
        addParticipationForTeam(ctx, teamB, teamA);
    }

    private void addParticipationForTeam(ProcessingContext ctx, TeamDto teamDto, TeamDto opponentDto) {
        Team represented = team(ctx, teamDto.name());
        Team opponent = team(ctx, opponentDto.name());
        for (String rawPlayerName : teamDto.players()) {
            String playerName = canonical(rawPlayerName);
            Player player = ctx.playerMap().get(playerName);
            if (player == null) {
                throw new BadRequestException("Player not found in match squad: " + playerName);
            }
            ctx.participationMap().computeIfAbsent(playerName + "_" + represented.getId(), ignored -> {
                MatchPlayerParticipation participation = new MatchPlayerParticipation();
                participation.setPlayer(player);
                participation.setMatch(ctx.match());
                participation.setSeason(ctx.season());
                participation.setTeamRepresented(represented);
                participation.setOppositionTeam(opponent);
                participation.setMatchType(ctx.matchType());
                participation.setMatchWon(isWinningTeam(ctx.match(), represented));
                participation.setPlayerOfTheMatch(Objects.equals(ctx.playerOfTheMatch(), playerName));
                return participation;
            });
        }
    }

    private Team matchTeam(Match match, String canonicalTeamName) {
        if (match.getTeamA() != null && canonical(match.getTeamA().getTeamName()).equals(canonicalTeamName)) {
            return match.getTeamA();
        }
        if (match.getTeamB() != null && canonical(match.getTeamB().getTeamName()).equals(canonicalTeamName)) {
            return match.getTeamB();
        }
        return null;
    }

    private void processBattingStats(ProcessingContext ctx, List<InningsDto> innings) {
        for (int i = 0; i < innings.size(); i++) {
            InningsDto inning = innings.get(i);
            if (inning.isSuperOver()) {
                continue;
            }

            int inningsNumber = i + 1; // absolute innings sequence, consistently used across all projections
            Team battingTeam = team(ctx, inning.battingTeam());
            Team bowlingTeam = team(ctx, inning.bowlingTeam());
            boolean battingFirst = isBattingFirstTeam(ctx.match(), battingTeam);

            for (Map.Entry<String, BattingStatDto> entry : safeMap(inning.battingStats()).entrySet()) {
                String playerName = canonical(entry.getKey());
                BattingStatDto stat = entry.getValue();
                if (stat == null) {
                    continue;
                }

                PlayerMatch playerMatch = getOrCreatePlayerMatch(
                        ctx, playerName, battingTeam, bowlingTeam, inningsNumber, battingFirst);
                playerMatch.setBatted(true);
                playerMatch.setBattingPosition(stat.battingPosition());
                playerMatch.setRunsScored(stat.runs());
                playerMatch.setBallsFaced(stat.balls());
                playerMatch.setFoursHit(stat.fours());
                playerMatch.setSixesHit(stat.sixes());

                if (stat.dismissal() != null) {
                    playerMatch.setOut(true);
                    playerMatch.setDismissalType(stat.dismissal().type());
                }
            }
        }
    }

    private void processBowlingStats(ProcessingContext ctx, List<InningsDto> innings) {
        for (int i = 0; i < innings.size(); i++) {
            InningsDto inning = innings.get(i);
            if (inning.isSuperOver()) {
                continue;
            }

            int inningsNumber = i + 1;
            Team battingTeam = team(ctx, inning.battingTeam());
            Team bowlingTeam = team(ctx, inning.bowlingTeam());
            boolean bowlingTeamBattedFirst = isBattingFirstTeam(ctx.match(), bowlingTeam);

            for (Map.Entry<String, BowlingStatDto> entry : safeMap(inning.bowlingStats()).entrySet()) {
                String playerName = canonical(entry.getKey());
                BowlingStatDto stat = entry.getValue();
                if (stat == null) {
                    continue;
                }

                PlayerMatch playerMatch = getOrCreatePlayerMatch(
                        ctx, playerName, bowlingTeam, battingTeam, inningsNumber, bowlingTeamBattedFirst);
                playerMatch.setBowled(true);
                playerMatch.setWicketsTaken(stat.wickets());
                playerMatch.setBallsBowled(stat.balls());
                playerMatch.setRunsConceded(stat.runs());
                playerMatch.setMaidensBowled(stat.maidens());
                playerMatch.setNoBallsBowled(stat.noBallsBowled());
                playerMatch.setWidesBowled(stat.widesBowled());
            }
        }
    }

    private void processFieldingStats(ProcessingContext ctx, List<InningsDto> innings) {
        for (int i = 0; i < innings.size(); i++) {
            InningsDto inning = innings.get(i);
            if (inning.isSuperOver()) {
                continue;
            }

            int inningsNumber = i + 1;
            Team battingTeam = team(ctx, inning.battingTeam());
            Team bowlingTeam = team(ctx, inning.bowlingTeam());
            boolean bowlingTeamBattedFirst = isBattingFirstTeam(ctx.match(), bowlingTeam);

            for (DismissalDto dismissal : safeMap(inning.dismissals()).values()) {
                if (dismissal == null || dismissal.type() == null) {
                    continue;
                }

                String bowler = canonical(dismissal.bowler());
                String fielder = canonical(dismissal.fielder());
                DismissalType type = dismissal.type();

                if (hasText(bowler)) {
                    PlayerMatch bowlerStats = getOrCreatePlayerMatch(
                            ctx, bowler, bowlingTeam, battingTeam, inningsNumber, bowlingTeamBattedFirst);
                    bowlerStats.setBowled(true);
                    incrementDismissalForBowler(bowlerStats, type);
                }

                if (hasText(fielder)) {
                    PlayerMatch fielderStats = getOrCreatePlayerMatch(
                            ctx, fielder, bowlingTeam, battingTeam, inningsNumber, bowlingTeamBattedFirst);
                    switch (type) {
                        case CAUGHT -> fielderStats.setCatchesTaken(fielderStats.getCatchesTaken() + 1);
                        case RUN_OUT -> fielderStats.setRunOuts(fielderStats.getRunOuts() + 1);
                        case STUMPED -> fielderStats.setStumpings(fielderStats.getStumpings() + 1);
                        default -> { }
                    }
                }
            }
        }
    }

    private void incrementDismissalForBowler(PlayerMatch stats, DismissalType type) {
        switch (type) {
            case BOWLED -> stats.setBowledDismissals(stats.getBowledDismissals() + 1);
            case CAUGHT -> stats.setCaughtDismissals(stats.getCaughtDismissals() + 1);
            case LBW -> stats.setLbwDismissals(stats.getLbwDismissals() + 1);
            case STUMPED -> stats.setStumpedDismissals(stats.getStumpedDismissals() + 1);
            case HIT_WICKET -> stats.setHitWicketDismissals(stats.getHitWicketDismissals() + 1);
            case RUN_OUT -> { }
            default -> stats.setSpecialWicketDismissals(stats.getSpecialWicketDismissals() + 1);
        }
    }

    private void processPartnershipsAndRivalries(ProcessingContext ctx, List<InningsDto> innings) {
        for (int i = 0; i < innings.size(); i++) {
            InningsDto inning = innings.get(i);
            if (inning.isSuperOver() || inning.ballByBall() == null || inning.ballByBall().isEmpty()) {
                continue;
            }

            Team battingTeam = team(ctx, inning.battingTeam());
            Team bowlingTeam = team(ctx, inning.bowlingTeam());
            boolean battingFirst = isBattingFirstTeam(ctx.match(), battingTeam);
            boolean bowlingTeamBattedFirst = isBattingFirstTeam(ctx.match(), bowlingTeam);
            int inningsNumber = i + 1;

            Player player1 = null;
            Player player2 = null;
            int partnershipNumber = 1;

            for (BallDto ball : inning.ballByBall()) {
                if (ball == null || ball.type() == null) {
                    continue;
                }

                if (ball.type() == BallType.RETIRE) {
                    Player retired = player(ctx, ball.striker());
                    if (Objects.equals(retired, player1)) {
                        player1 = null;
                    } else if (Objects.equals(retired, player2)) {
                        player2 = null;
                    }
                    partnershipNumber++;
                    continue;
                }

                Player striker = player(ctx, ball.striker());
                Player nonStriker = player(ctx, ball.nonStriker());
                Player bowler = player(ctx, ball.bowler());
                if (striker == null || bowler == null) {
                    throw new BadRequestException("Ball-by-ball contains an unknown striker or bowler");
                }

                Player[] pair = resolvePartnershipPair(player1, player2, striker, nonStriker);
                player1 = pair[0];
                player2 = pair[1];
                if (player1 == null || player2 == null) {
                    // A non-striker can be absent in edge states such as retirement transitions.
                    // Rivalry still remains valid, but a two-player partnership cannot be recorded.
                    continue;
                }

                // Stable ordering prevents duplicate partnership rows when strike rotates.
                if (player1.getName().compareTo(player2.getName()) > 0) {
                    Player temp = player1;
                    player1 = player2;
                    player2 = temp;
                }

                String partnershipKey = player1.getName() + "_" + player2.getName() + "_" + inningsNumber + "_" + partnershipNumber;
                PlayerPartnerships partnership = ctx.partnershipMap().get(partnershipKey);
                if (partnership == null) {
                    partnership = new PlayerPartnerships();
                    partnership.setPlayer1(player1);
                    partnership.setPlayer2(player2);
                    partnership.setInningsNumber(inningsNumber);
                    partnership.setMatchType(ctx.matchType());
                    partnership.setSeason(ctx.season());
                    partnership.setMatch(ctx.match());
                    partnership.setTeamRepresented(battingTeam);
                    partnership.setPartnershipNumber(partnershipNumber);
                    partnership.setBattingFirst(battingFirst);
                    partnership.setMatchWon(isWinningTeam(ctx.match(), battingTeam));
                    ctx.partnershipMap().put(partnershipKey, partnership);
                }

                String rivalryKey = striker.getName() + "_" + bowler.getName() + "_" + inningsNumber;
                PlayerRivalry rivalry = ctx.rivalryMap().get(rivalryKey);
                if (rivalry == null) {
                    rivalry = new PlayerRivalry();
                    rivalry.setBatsman(striker);
                    rivalry.setBowler(bowler);
                    rivalry.setInningsNumber(inningsNumber);
                    rivalry.setMatchType(ctx.matchType());
                    rivalry.setMatch(ctx.match());
                    rivalry.setSeason(ctx.season());
                    ctx.rivalryMap().put(rivalryKey, rivalry);
                }

                boolean legalDelivery = isLegalDelivery(ball.type(), ctx.rules());
                int totalRuns = Math.max(0, ball.runs());
                int batterRuns = batterRuns(ball, ctx.rules());

                if (legalDelivery) {
                    PlayerMatch batterMatch = getOrCreatePlayerMatch(
                            ctx, striker.getName(), battingTeam, bowlingTeam, inningsNumber, battingFirst);
                    PlayerMatch bowlerMatch = getOrCreatePlayerMatch(
                            ctx, bowler.getName(), bowlingTeam, battingTeam, inningsNumber, bowlingTeamBattedFirst);

                    if (batterRuns == 0) {
                        batterMatch.setDotBallsPlayed(batterMatch.getDotBallsPlayed() + 1);
                    }
                    if (totalRuns == 0) {
                        bowlerMatch.setDotBallsBowled(bowlerMatch.getDotBallsBowled() + 1);
                    }

                    partnership.setBallsFaced(partnership.getBallsFaced() + 1);
                    rivalry.setBallsFaced(rivalry.getBallsFaced() + 1);
                    if (Objects.equals(striker, partnership.getPlayer1())) {
                        partnership.setPlayer1BallsFaced(partnership.getPlayer1BallsFaced() + 1);
                    } else {
                        partnership.setPlayer2BallsFaced(partnership.getPlayer2BallsFaced() + 1);
                    }

                    if (batterRuns == 0) {
                        partnership.setDotBalls(partnership.getDotBalls() + 1);
                        rivalry.setDotBalls(rivalry.getDotBalls() + 1);
                        if (Objects.equals(striker, partnership.getPlayer1())) {
                            partnership.setPlayer1DotBalls(partnership.getPlayer1DotBalls() + 1);
                        } else {
                            partnership.setPlayer2DotBalls(partnership.getPlayer2DotBalls() + 1);
                        }
                    }
                }

                partnership.setRunsScored(partnership.getRunsScored() + totalRuns);
                rivalry.setRunsScored(rivalry.getRunsScored() + batterRuns);

                if (ball.type() != BallType.WIDE) {
                    addBatterRunsToPartnership(partnership, striker, batterRuns);
                    if (batterRuns == 4) {
                        partnership.setFoursHit(partnership.getFoursHit() + 1);
                        rivalry.setFoursHit(rivalry.getFoursHit() + 1);
                        incrementPlayerBoundary(partnership, striker, true);
                    } else if (batterRuns == 6) {
                        partnership.setSixesHit(partnership.getSixesHit() + 1);
                        rivalry.setSixesHit(rivalry.getSixesHit() + 1);
                        incrementPlayerBoundary(partnership, striker, false);
                    }
                }

                if (ball.isWicket() && ball.wicket() != null) {
                    Player outBatsman = player(ctx, ball.wicket().outBatsman());
                    partnership.setPartnershipBroken(true);
                    partnership.setWhoGotOut(outBatsman);

                    if (Objects.equals(outBatsman, striker)) {
                        rivalry.setBatsmanDismissed(true);
                        rivalry.setDismissalType(parseDismissalType(ball.wicket().type()));
                    }

                    if (Objects.equals(outBatsman, player1)) {
                        player1 = null;
                    } else if (Objects.equals(outBatsman, player2)) {
                        player2 = null;
                    }
                    partnershipNumber++;
                }
            }
        }
    }

    private Player[] resolvePartnershipPair(Player player1, Player player2, Player striker, Player nonStriker) {
        if (player1 == null && player2 == null) {
            return new Player[]{striker, nonStriker};
        }
        if (player1 == null) {
            return Objects.equals(player2, striker)
                    ? new Player[]{nonStriker, player2}
                    : new Player[]{striker, player2};
        }
        if (player2 == null) {
            return Objects.equals(player1, striker)
                    ? new Player[]{player1, nonStriker}
                    : new Player[]{player1, striker};
        }
        return new Player[]{player1, player2};
    }

    private void addBatterRunsToPartnership(PlayerPartnerships partnership, Player striker, int runs) {
        if (Objects.equals(striker, partnership.getPlayer1())) {
            partnership.setPlayer1Runs(partnership.getPlayer1Runs() + runs);
        } else {
            partnership.setPlayer2Runs(partnership.getPlayer2Runs() + runs);
        }
    }

    private void incrementPlayerBoundary(PlayerPartnerships partnership, Player striker, boolean four) {
        if (Objects.equals(striker, partnership.getPlayer1())) {
            if (four) partnership.setPlayer1FoursHit(partnership.getPlayer1FoursHit() + 1);
            else partnership.setPlayer1SixesHit(partnership.getPlayer1SixesHit() + 1);
        } else {
            if (four) partnership.setPlayer2FoursHit(partnership.getPlayer2FoursHit() + 1);
            else partnership.setPlayer2SixesHit(partnership.getPlayer2SixesHit() + 1);
        }
    }

    private boolean isLegalDelivery(BallType type, RulesDto rules) {
        if (type == BallType.WIDE) {
            return rules == null || rules.wide() == null || !rules.wide().extraBall();
        }
        if (type == BallType.NO_BALL) {
            return rules == null || rules.noBall() == null || !rules.noBall().extraBall();
        }
        return true;
    }

    private int batterRuns(BallDto ball, RulesDto rules) {
        if (ball.type() == BallType.WIDE || ball.type() == BallType.BYE) {
            return 0; // wides and byes never belong to the batter
        }
        int penalty = 0;
        if (ball.type() == BallType.NO_BALL && rules != null && rules.noBall() != null && rules.noBall().extraRun()) {
            penalty = 1;
        }
        return Math.max(0, ball.runs() - penalty);
    }

    private DismissalType parseDismissalType(String value) {
        if (!hasText(value)) {
            return DismissalType.SPECIAL;
        }
        try {
            return DismissalType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return DismissalType.SPECIAL;
        }
    }

    private void saveAllProcessedStats(ProcessingContext ctx) {
        List<Player> newPlayers = ctx.playerMap().values().stream()
                .filter(player -> player.getId() == null)
                .toList();
        if (!newPlayers.isEmpty()) {
            playerService.saveAllPlayers(newPlayers);
        }

        // Existing players were loaded in the current transaction and new players now
        // have generated ids, so there is no reason to merge every squad member again.
        List<Player> allPlayers = new ArrayList<>(ctx.playerMap().values());

        List<SeasonPlayer> newSeasonPlayers = allPlayers.stream()
                .filter(p -> !ctx.seasonPlayerIds().contains(p.getId()))
                .map(p -> {
                    SeasonPlayer sp = new SeasonPlayer();
                    sp.setPlayer(p);
                    sp.setSeason(ctx.season());
                    return sp;
                })
                .toList();

        if (!newSeasonPlayers.isEmpty()) {
            seasonPlayerRepository.saveAll(newSeasonPlayers);
        }
        if (!ctx.playerTeams().isEmpty()) {
            playerTeamService.saveListOfPlayerTeams(ctx.playerTeams());
        }
        if (!ctx.participationMap().isEmpty()) {
            matchPlayerParticipationRepository.saveAll(ctx.participationMap().values());
        }
        if (!ctx.playerMatchMap().isEmpty()) {
            playerMatchRepository.saveAll(ctx.playerMatchMap().values());
        }
        if (!ctx.partnershipMap().isEmpty()) {
            playerPartnershipsRepository.saveAll(ctx.partnershipMap().values());
        }
        if (!ctx.rivalryMap().isEmpty()) {
            playerRivalryRepository.saveAll(ctx.rivalryMap().values());
        }
    }

    private PlayerMatch getOrCreatePlayerMatch(ProcessingContext ctx, String rawPlayerName,
                                                Team teamRepresented, Team oppositionTeam,
                                                int inningsNumber, boolean battingFirst) {
        String playerName = canonical(rawPlayerName);
        Player player = ctx.playerMap().get(playerName);
        if (player == null) {
            throw new BadRequestException("Player not found in match squad: " + playerName);
        }
        if (teamRepresented == null || oppositionTeam == null) {
            throw new BadRequestException("Unable to resolve team context for player: " + playerName);
        }

        String key = playerName + "_" + teamRepresented.getId() + "_" + inningsNumber;
        return ctx.playerMatchMap().computeIfAbsent(key, ignored -> {
            PlayerMatch pm = new PlayerMatch();
            pm.setPlayer(player);
            pm.setMatch(ctx.match());
            pm.setSeason(ctx.season());
            pm.setTeamRepresented(teamRepresented);
            pm.setOppositionTeam(oppositionTeam);
            pm.setMatchType(ctx.matchType());
            pm.setInningsNumber(inningsNumber);
            pm.setMatchWon(isWinningTeam(ctx.match(), teamRepresented));
            pm.setPlayerOfTheMatch(Objects.equals(ctx.playerOfTheMatch(), playerName));
            pm.setBattingFirst(battingFirst);
            pm.setBowlingFirst(!battingFirst);
            return pm;
        });
    }

    private boolean isWinningTeam(Match match, Team team) {
        return match.getWinnerTeam() != null && team != null
                && Objects.equals(match.getWinnerTeam().getId(), team.getId());
    }

    private boolean isBattingFirstTeam(Match match, Team team) {
        return match.getBattingFirstTeam() != null && team != null
                && Objects.equals(match.getBattingFirstTeam().getId(), team.getId());
    }

    private Team team(ProcessingContext ctx, String name) {
        Team team = ctx.teamMap().get(canonical(name));
        if (team == null) {
            throw new BadRequestException("Unknown team in innings: " + name);
        }
        return team;
    }

    private Player player(ProcessingContext ctx, String name) {
        if (!hasText(name)) {
            return null;
        }
        return ctx.playerMap().get(canonical(name));
    }

    private String canonical(String value) {
        return NameNormalizer.normalize(value);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private <K, V> Map<K, V> safeMap(Map<K, V> map) {
        return map == null ? Map.of() : map;
    }
}
