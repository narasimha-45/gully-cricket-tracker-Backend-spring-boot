package com.gullycricket.backend.matches.service;

import com.gullycricket.backend.common.util.NameNormalizer;
import com.gullycricket.backend.matches.DTOs.*;
import com.gullycricket.backend.matches.entity.Match;
import com.gullycricket.backend.players.entity.*;
import com.gullycricket.backend.players.repository.PlayerMatchRepository;
import com.gullycricket.backend.players.repository.PlayerPartnershipsRepository;
import com.gullycricket.backend.players.repository.PlayerRivalryRepository;
import com.gullycricket.backend.players.service.PlayerService;
import com.gullycricket.backend.seasons.entity.Season;
import com.gullycricket.backend.seasons.entity.SeasonPlayer;
import com.gullycricket.backend.seasons.repository.SeasonPlayerRepository;
import com.gullycricket.backend.seasons.service.SeasonService;
import com.gullycricket.backend.teams.entity.PlayerTeam;
import com.gullycricket.backend.teams.entity.Team;
import com.gullycricket.backend.teams.reposistory.PlayerTeamRepository;
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

    private final SeasonService seasonService;
    private final TeamService teamService;
    private final PlayerService playerService;
    private final PlayerTeamService playerTeamService;
    private final SeasonPlayerRepository seasonPlayerRepository;
    private final PlayerMatchRepository playerMatchRepository;
    private final PlayerPartnershipsRepository playerPartnershipsRepository;
    private final PlayerRivalryRepository playerRivalryRepository;
    private final PlayerTeamRepository playerTeamRepository;

    private ProcessingContext getProcessingContext(Match match, MatchDataDto matchData, Season season) {
        String winningTeam = matchData.result() != null ? matchData.result().winner() : null;
        String playerOfTheMatch = matchData.result() != null ? matchData.result().manOfTheMatch() : null;

        // Fetch only the players in this match — ONE targeted query
        List<String> allPlayerNames = new ArrayList<>();
        allPlayerNames.addAll(matchData.teams().get("teamA").players());
        allPlayerNames.addAll(matchData.teams().get("teamB").players());

        Map<String, Player> playerMap = playerService.getPlayersByNameIn(allPlayerNames).stream().collect(Collectors.toMap(Player::getName, p -> p));

        // Prefetch which players are already in this season — ONE query
        Set<String> seasonPlayerIds = seasonPlayerRepository.findPlayerIdsBySeasonId(season.getId());

        return new ProcessingContext(match, season, matchData.matchType(), winningTeam, playerOfTheMatch, matchData.rules(), playerMap, new HashMap<>(), new HashMap<>(), seasonPlayerIds, new HashMap<>(), new HashMap<>(), new HashMap<>(), new ArrayList<>());
    }

    public void processPlayerStats(Match match, MatchDataDto matchData) {
        Season season = seasonService.getSeasonById(matchData.seasonId());
        ProcessingContext ctx = getProcessingContext(match, matchData, season);
        processTeams(ctx, matchData.teams());
        processBattingStats(ctx, matchData.innings());
        processBowlingStats(ctx, matchData.innings());
        processFieldingStats(ctx, matchData.innings());
        processPartnershipsAndRivalries(ctx, matchData.innings());
        saveAllProcessedStats(ctx);
    }

    private void processTeams(ProcessingContext ctx, Map<String, TeamDto> teams) {
        processSingleTeam(ctx, teams.get("teamA"));
        processSingleTeam(ctx, teams.get("teamB"));
    }

    private void processSingleTeam(ProcessingContext ctx, TeamDto teamDto) {
        String teamName = NameNormalizer.normalize(teamDto.name());

        Team team = teamService.getTeamByName(teamName);
        if (team == null) {
            team = new Team();
            team.setTeamName(teamName);
        }
        team.getSeasonsPlayed().add(ctx.season());
        team = teamService.saveTeam(team);
        ctx.teamMap().put(teamName, team);

        // ONE query — existing PlayerTeam rows for this team+season
        Set<String> existingPlayerNames = playerTeamService.findExistingPlayerNamesByTeamAndSeason(team, ctx.season());

        for (String playerName : teamDto.players()) {
            Player player = ctx.playerMap().get(playerName);

            if (player == null) {
                player = new Player();
                player.setName(playerName);
            }

            ctx.playerMap().put(playerName, player);
            ctx.playerTeamMap().put(playerName, team);

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

    private void processBattingStats(ProcessingContext ctx, List<InningsDto> innings) {
        for (int i = 0; i < innings.size(); i++) {
            InningsDto inning = innings.get(i);

            if (inning.isSuperOver()) {
                continue;
            }

            int inningsNumber = i  + 1;
            Team battingTeam = ctx.teamMap().get(inning.battingTeam());
            Team bowlingTeam = ctx.teamMap().get(inning.bowlingTeam());
            boolean battingFirst = i % 2 == 0;
            int battingPosition = 1;

            for (Map.Entry<String, BattingStatDto> entry : inning.battingStats().entrySet()) {
                String player = entry.getKey();
                BattingStatDto stat = entry.getValue();

                PlayerMatch playerMatch = getOrCreatePlayerMatch(ctx, player, battingTeam, bowlingTeam, inningsNumber, battingFirst);

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

            int inningsNumber = i  + 1;
            Team battingTeam = ctx.teamMap().get(inning.battingTeam());
            Team bowlingTeam = ctx.teamMap().get(inning.bowlingTeam());
            boolean battingFirst = i % 2 == 0;

            for (Map.Entry<String, BowlingStatDto> entry : inning.bowlingStats().entrySet()) {
                String player = entry.getKey();
                BowlingStatDto stat = entry.getValue();

                PlayerMatch playerMatch = getOrCreatePlayerMatch(ctx, player, bowlingTeam, battingTeam, inningsNumber, !battingFirst);

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
            int inningsNumber = i  + 1;
            InningsDto inning = innings.get(i);

            if (inning.isSuperOver()) {
                continue;
            }

            Team battingTeam = ctx.teamMap().get(inning.battingTeam());
            Team bowlingTeam = ctx.teamMap().get(inning.bowlingTeam());

            Map<String, DismissalDto> dismissals = inning.dismissals();

            for (DismissalDto dismissal : dismissals.values()) {
                String bowler = dismissal.bowler();
                String fielder = dismissal.fielder();
                DismissalType dismissalType = dismissal.type();

                if (bowler != null && !bowler.isBlank()) {
                    PlayerMatch bowlerStats = getOrCreatePlayerMatch(ctx, bowler, bowlingTeam, battingTeam, inningsNumber, false);
                    bowlerStats.setBowled(true);

                    switch (dismissalType) {
                        case BOWLED -> bowlerStats.setBowledDismissals(bowlerStats.getBowledDismissals() + 1);
                        case CAUGHT -> bowlerStats.setCaughtDismissals(bowlerStats.getCaughtDismissals() + 1);
                        case LBW -> bowlerStats.setLbwDismissals(bowlerStats.getLbwDismissals() + 1);
                        case STUMPED -> bowlerStats.setStumpedDismissals(bowlerStats.getStumpedDismissals() + 1);
                        case HIT_WICKET -> bowlerStats.setHitWicketDismissals(bowlerStats.getHitWicketDismissals() + 1);
                        case RUN_OUT -> {
                        }
                        default -> bowlerStats.setSpecialWicketDismissals(bowlerStats.getSpecialWicketDismissals() + 1);
                    }
                }

                if (fielder != null && !fielder.isBlank()) {
                    PlayerMatch fielderStats = getOrCreatePlayerMatch(ctx, fielder, bowlingTeam, battingTeam, inningsNumber, false);

                    switch (dismissalType) {
                        case CAUGHT -> fielderStats.setCatchesTaken(fielderStats.getCatchesTaken() + 1);
                        case RUN_OUT -> fielderStats.setRunOuts(fielderStats.getRunOuts() + 1);
                        case STUMPED -> fielderStats.setStumpings(fielderStats.getStumpings() + 1);
                        default -> {
                        }
                    }
                }
            }
        }
    }

    private void processPartnershipsAndRivalries(ProcessingContext ctx, List<InningsDto> innings) {
        for (int i = 0; i < innings.size(); i++) {
            InningsDto inning = innings.get(i);

            if (inning.isSuperOver()) {
                continue;
            }

            List<BallDto> ballByBall = inning.ballByBall();
            Team battingTeam = ctx.teamMap().get(inning.battingTeam());
            int inningsNumber = i / 2 + 1;
            Player player1 = null;
            Player player2 = null;
            PlayerPartnerships playerPartnerships;
            PlayerRivalry playerRivalry;
            int partnershipNumber = 1;

            for (BallDto ballDto : ballByBall) {
                BallType ballType = ballDto.type();

                if (ballType == BallType.RETIRE) {
                    Player retiredPlayer = ctx.playerMap().get(ballDto.striker());
                    if (Objects.equals(retiredPlayer, player1)) {
                        player1 = null;
                    } else {
                        player2 = null;
                    }
                    partnershipNumber++;
                    continue;
                }

                if (player1 == null && player2 == null) {
                    player1 = ctx.playerMap().get(ballDto.striker());
                    player2 = ctx.playerMap().get(ballDto.nonStriker());
                } else if (player1 == null) {
                    if (Objects.equals(player2.getName(), ballDto.striker())) {
                        player1 = ctx.playerMap().get(ballDto.nonStriker());
                    } else {
                        player1 = ctx.playerMap().get(ballDto.striker());
                    }
                } else if (player2 == null) {
                    if (Objects.equals(player1.getName(), ballDto.striker())) {
                        player2 = ctx.playerMap().get(ballDto.nonStriker());
                    } else {
                        player2 = ctx.playerMap().get(ballDto.striker());
                    }
                }

                if (player1.getName().compareTo(player2.getName()) > 0) {
                    Player temp = player1;
                    player1 = player2;
                    player2 = temp;
                }

                String partnershipKey = player1.getName() + "_" + player2.getName() + "_" + inningsNumber + "_" + partnershipNumber;

                if (ctx.partnershipMap().containsKey(partnershipKey)) {
                    playerPartnerships = ctx.partnershipMap().get(partnershipKey);
                } else {
                    playerPartnerships = new PlayerPartnerships();
                    playerPartnerships.setPlayer1(player1);
                    playerPartnerships.setPlayer2(player2);
                    playerPartnerships.setInningsNumber(inningsNumber);
                    playerPartnerships.setMatchType(ctx.matchType());
                    playerPartnerships.setSeason(ctx.season());
                    playerPartnerships.setMatch(ctx.match());
                    playerPartnerships.setTeamRepresented(battingTeam);
                    playerPartnerships.setPartnershipNumber(partnershipNumber);
                    playerPartnerships.setBattingFirst(i % 2 == 0);
                    ctx.partnershipMap().put(partnershipKey, playerPartnerships);
                }

                Player striker = ctx.playerMap().get(ballDto.striker());
                Player bowler = ctx.playerMap().get(ballDto.bowler());

                String batterBowlerKey = striker.getName() + "_" + bowler.getName() + "_" + inningsNumber;

                if (ctx.rivalryMap().containsKey(batterBowlerKey)) {
                    playerRivalry = ctx.rivalryMap().get(batterBowlerKey);
                } else {
                    playerRivalry = new PlayerRivalry();
                    playerRivalry.setBatsman(striker);
                    playerRivalry.setBowler(bowler);
                    playerRivalry.setInningsNumber(inningsNumber);
                    playerRivalry.setMatchType(ctx.matchType());
                    playerRivalry.setMatch(ctx.match());
                    playerRivalry.setSeason(ctx.season());
                    ctx.rivalryMap().put(batterBowlerKey, playerRivalry);
                }

                boolean isBallCount = ballType != BallType.WIDE;
                int runs = ballDto.runs();
                int wideExtraPenalty = (ballType == BallType.WIDE && ctx.rules().wide().extraRun()) ? 1 : 0;
                int noBallExtraPenalty = (ballType == BallType.NO_BALL && ctx.rules().noBall().extraRun()) ? 1 : 0;
                int extraPenaltyRun = wideExtraPenalty + noBallExtraPenalty;
                int batsmanRuns = runs - extraPenaltyRun;

                if (isBallCount) {

                    // dot balls are counted for both the batter and the bowler, so we need to update their stats accordingly
                    PlayerMatch batterMatch = getOrCreatePlayerMatch(
                            ctx,
                            striker.getName(),
                            battingTeam,
                            ctx.teamMap().get(inning.bowlingTeam()),
                            inningsNumber,
                            i % 2 == 0
                    );

                    PlayerMatch bowlerMatch = getOrCreatePlayerMatch(
                            ctx,
                            bowler.getName(),
                            ctx.teamMap().get(inning.bowlingTeam()),
                            battingTeam,
                            inningsNumber,
                            !(i % 2 == 0)
                    );

                    if (batsmanRuns == 0) {
                        batterMatch.setDotBallsPlayed(batterMatch.getDotBallsPlayed() + 1);
                        bowlerMatch.setDotBallsBowled(bowlerMatch.getDotBallsBowled() + 1);
                    }

                    playerPartnerships.setBallsFaced(playerPartnerships.getBallsFaced() + 1);
                    if (Objects.equals(striker, player1)) {
                        playerPartnerships.setPlayer1BallsFaced(playerPartnerships.getPlayer1BallsFaced() + 1);
                    } else {
                        playerPartnerships.setPlayer2BallsFaced(playerPartnerships.getPlayer2BallsFaced() + 1);
                    }
                    playerRivalry.setBallsFaced(playerRivalry.getBallsFaced() + 1);

                    if (batsmanRuns == 0) {
                        playerPartnerships.setDotBalls(playerPartnerships.getDotBalls() + 1);
                        playerRivalry.setDotBalls(playerRivalry.getDotBalls() + 1);
                        if (Objects.equals(striker, player1)) {
                            playerPartnerships.setPlayer1DotBalls(playerPartnerships.getPlayer1DotBalls() + 1);
                        } else {
                            playerPartnerships.setPlayer2DotBalls(playerPartnerships.getPlayer2DotBalls() + 1);
                        }
                    }
                }

                playerPartnerships.setRunsScored(playerPartnerships.getRunsScored() + runs);
                playerRivalry.setRunsScored(playerRivalry.getRunsScored() + batsmanRuns);

                if (ballType != BallType.WIDE) {
                    if (Objects.equals(striker, player1)) {
                        playerPartnerships.setPlayer1Runs(playerPartnerships.getPlayer1Runs() + batsmanRuns);
                    } else {
                        playerPartnerships.setPlayer2Runs(playerPartnerships.getPlayer2Runs() + batsmanRuns);
                    }

                    if (batsmanRuns == 4) {
                        playerPartnerships.setFoursHit(playerPartnerships.getFoursHit() + 1);
                        playerRivalry.setFoursHit(playerRivalry.getFoursHit() + 1);
                        if (Objects.equals(striker, player1)) {
                            playerPartnerships.setPlayer1FoursHit(playerPartnerships.getPlayer1FoursHit() + 1);
                        } else {
                            playerPartnerships.setPlayer2FoursHit(playerPartnerships.getPlayer2FoursHit() + 1);
                        }
                    }

                    if (batsmanRuns == 6) {
                        playerPartnerships.setSixesHit(playerPartnerships.getSixesHit() + 1);
                        playerRivalry.setSixesHit(playerRivalry.getSixesHit() + 1);
                        if (Objects.equals(striker, player1)) {
                            playerPartnerships.setPlayer1SixesHit(playerPartnerships.getPlayer1SixesHit() + 1);
                        } else {
                            playerPartnerships.setPlayer2SixesHit(playerPartnerships.getPlayer2SixesHit() + 1);
                        }
                    }
                }

                if (ballType == BallType.WICKET && ballDto.wicket() != null) {
                    Player outBatsman = ctx.playerMap().get(ballDto.wicket().outBatsman());

                    playerPartnerships.setPartnershipBroken(true);
                    playerPartnerships.setWhoGotOut(outBatsman);

                    if (Objects.equals(outBatsman, striker)) {
                        playerRivalry.setBatsmanDismissed(true);
                        playerRivalry.setDismissalType(DismissalType.valueOf(ballDto.wicket().type()));
                    }

                    if (Objects.equals(outBatsman, player1)) {
                        player1 = null;
                    } else {
                        player2 = null;
                    }

                    partnershipNumber++;
                }
            }
        }
    }

    private void saveAllProcessedStats(ProcessingContext ctx) {
        // 1. Save players first — assigns IDs to genuinely new players
        List<Player> savedPlayers = playerService.saveAllPlayers(ctx.playerMap().values().stream().toList());

        // 2. Build SeasonPlayer rows — only for players not already in this season
        List<SeasonPlayer> newSeasonPlayers = savedPlayers.stream().filter(p -> !ctx.seasonPlayerIds().contains(p.getId())).map(p -> {
            SeasonPlayer sp = new SeasonPlayer();
            sp.setPlayer(p);
            sp.setSeason(ctx.season());
            return sp;
        }).toList();

        seasonPlayerRepository.saveAll(newSeasonPlayers);

        playerTeamService.saveListOfPlayerTeams(ctx.playerTeams());

        // 3. Save all stat rows
        playerMatchRepository.saveAll(ctx.playerMatchMap().values());
        playerPartnershipsRepository.saveAll(ctx.partnershipMap().values());
        playerRivalryRepository.saveAll(ctx.rivalryMap().values());
    }

    private PlayerMatch getOrCreatePlayerMatch(ProcessingContext ctx, String playerName, Team teamRepresented, Team oppositionTeam, int inningsNumber, boolean battingFirst) {
        String key = playerName + "_" + teamRepresented.getId() + "_" + inningsNumber;
        PlayerMatch playerMatch = ctx.playerMatchMap().get(key);

        if (playerMatch == null) {
            playerMatch = new PlayerMatch();
            playerMatch.setPlayer(ctx.playerMap().get(playerName));
            playerMatch.setMatch(ctx.match());
            playerMatch.setSeason(ctx.season());
            playerMatch.setTeamRepresented(teamRepresented);
            playerMatch.setOppositionTeam(oppositionTeam);
            playerMatch.setMatchType(ctx.matchType());
            playerMatch.setInningsNumber(inningsNumber);
            playerMatch.setMatchWon(ctx.winningTeam().equals(teamRepresented.getTeamName()));
            playerMatch.setPlayerOfTheMatch(Objects.equals(ctx.playerOfTheMatch(), playerName));
            playerMatch.setBattingFirst(battingFirst);
            playerMatch.setBowlingFirst(!battingFirst);
            ctx.playerMatchMap().put(key, playerMatch);
        }

        return playerMatch;
    }
}