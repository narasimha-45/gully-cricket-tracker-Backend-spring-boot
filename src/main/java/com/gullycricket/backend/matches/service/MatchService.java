package com.gullycricket.backend.matches.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gullycricket.backend.common.util.NameNormalizer;
import com.gullycricket.backend.matches.DTOs.BallDto;
import com.gullycricket.backend.matches.DTOs.BattingStatDto;
import com.gullycricket.backend.matches.DTOs.BowlingStatDto;
import com.gullycricket.backend.matches.DTOs.DismissalDto;
import com.gullycricket.backend.matches.DTOs.InningsDto;
import com.gullycricket.backend.matches.DTOs.MatchDataDto;
import com.gullycricket.backend.matches.DTOs.MatchResponseDto;
import com.gullycricket.backend.matches.DTOs.ResultDto;
import com.gullycricket.backend.matches.DTOs.TeamDto;
import com.gullycricket.backend.matches.DTOs.WicketDto;
import com.gullycricket.backend.matches.entity.Match;
import com.gullycricket.backend.matches.entity.MatchInningsSummary;
import com.gullycricket.backend.matches.entity.MatchStatus;
import com.gullycricket.backend.matches.entity.MatchType;
import com.gullycricket.backend.matches.respository.MatchRepository;
import com.gullycricket.backend.seasons.entity.Season;
import com.gullycricket.backend.seasons.repository.SeasonRepository;
import com.gullycricket.backend.teams.entity.Team;
import com.gullycricket.backend.teams.service.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final SeasonRepository seasonRepository;
    private final ObjectMapper objectMapper;
    private final ProcessPlayerStatsService processPlayerStatsService;
    private final TeamService teamService;

    @Transactional(readOnly = true)
    public MatchResponseDto getMatchById(String matchId) {
        log.info("Fetching match with id: {}", matchId);

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found: " + matchId));

        log.info("Match found with id: {}", matchId);
        return new MatchResponseDto(
                match.getId(),
                match.getSeason().getId(),
                match.getMatchData()
        );
    }

    @Transactional
    public MatchResponseDto saveMatch(JsonNode matchData) {
        log.info("Creating new match");

        MatchDataDto dto = objectMapper.convertValue(matchData, MatchDataDto.class);

        // Every player name that will ever be used as a lookup/grouping key downstream
        // (team squads, batting/bowling stat maps, dismissals, ball-by-ball, man of the
        // match) is normalized here in one place, before any Player is looked up or
        // created. This is what keeps "Virat Kohli", "virat kohli ", and "VIRAT KOHLI"
        // resolving to the exact same Player row instead of three different ones — for
        // both live match creation and the Mongo migration, since migration calls this
        // same method.
        dto = normalizePlayerNames(dto);

        // Re-serialize so the stored jsonb blob reflects the same normalized names as
        // everything derived from it — otherwise getMatchById would keep returning the
        // original casing/spacing even though every Player/PlayerMatch row is normalized.
        JsonNode normalizedMatchData = objectMapper.valueToTree(dto);

        MatchDataDto finalDto = dto;
        Season season = seasonRepository.findById(finalDto.seasonId())
                .orElseThrow(() -> new RuntimeException("Season not found: " + finalDto.seasonId()));

        season.setMatchesPlayed(season.getMatchesPlayed() + 1);

        boolean hasSuperOver = dto.innings().stream()
                .anyMatch(InningsDto::isSuperOver);

        List<InningsDto> regularInnings = dto.innings().stream()
                .filter(i -> !i.isSuperOver())
                .toList();

        String teamAName = dto.teams().get("teamA").name();
        String teamBName = dto.teams().get("teamB").name();
        String winner = dto.result().winner(); // null => draw (Test) or tie (limited-overs)

        Team teamAEntity = resolveTeam(teamAName, season);
        Team teamBEntity = resolveTeam(teamBName, season);

        int teamASquadSize = dto.teams().get("teamA").players().size();
        int teamBSquadSize = dto.teams().get("teamB").players().size();

        // Whoever's innings appears first in the innings list batted first
        // overall — true for every format, not just Test.
        String battingFirstTeamName = regularInnings.getFirst().battingTeam();
        boolean teamABattedFirst = battingFirstTeamName.equalsIgnoreCase(teamAName);
        Team battingFirstTeamEntity = teamABattedFirst ? teamAEntity : teamBEntity;
        Team battingSecondTeamEntity = teamABattedFirst ? teamBEntity : teamAEntity;

        Match match = new Match();
        match.setSeason(season);
        match.setMatchData(normalizedMatchData);
        match.setStatus(MatchStatus.COMPLETED);
        match.setTeamA(teamAEntity);
        match.setTeamB(teamBEntity);
        match.setMatchType(dto.matchType());
        match.setTotalOvers(dto.totalOvers());
        match.setSuperOver(hasSuperOver);
        match.setBattingFirstTeam(battingFirstTeamEntity);
        match.setBattingSecondTeam(battingSecondTeamEntity);
        match.setCompletedAt(LocalDateTime.now());

        if (winner != null && !winner.isBlank()) {
            match.setIsBattingFirstTeamWon(winner.equalsIgnoreCase(battingFirstTeamName));
        }

        if (dto.matchType() == MatchType.TEST) {
            applyTestResult(match, regularInnings, teamAName, teamBName, teamAEntity, teamBEntity,
                    winner, teamASquadSize, teamBSquadSize);
        } else {
            applyLimitedOversResult(match, regularInnings, teamAName, teamBName, teamAEntity, teamBEntity,
                    winner, hasSuperOver, teamASquadSize, teamBSquadSize, battingFirstTeamName);
        }

        log.info("match data before saving: {}", normalizedMatchData);

        Match savedMatch = matchRepository.save(match);

        log.info("Match created with id: {}", savedMatch.getId());

        processPlayerStatsService.processPlayerStats(savedMatch, dto);
        return new MatchResponseDto(
                savedMatch.getId(),
                savedMatch.getSeason().getId(),
                savedMatch.getMatchData()
        );
    }

    // Mirrors the get-or-create logic in ProcessPlayerStatsService.processSingleTeam.
    // Safe to call from both places — lookup is by name, so this never creates duplicates.
    private Team resolveTeam(String teamName, Season season) {
        String normalizedName = NameNormalizer.normalize(teamName);

        Team team = teamService.getTeamByName(normalizedName);
        if (team == null) {
            team = new Team();
            team.setTeamName(normalizedName);
        }

        team.getSeasonsPlayed().add(season);
        return teamService.saveTeam(team);
    }

    /**
     * Limited-overs (single innings per team): flat score fields on Match are the
     * source of truth. Each innings is matched to a team by name, not by list
     * position — position isn't guaranteed to line up with teamA/teamB.
     */
    private void applyLimitedOversResult(Match match, List<InningsDto> regularInnings,
                                         String teamAName, String teamBName,
                                         Team teamAEntity, Team teamBEntity,
                                         String winner, boolean hasSuperOver,
                                         int teamASquadSize, int teamBSquadSize,
                                         String battingFirstTeamName) {

        for (InningsDto inning : regularInnings) {
            if (inning.battingTeam().equalsIgnoreCase(teamAName)) {
                match.setTeamAScore(inning.totalRuns());
                match.setTeamAWickets(inning.wickets());
                match.setTeamABallsFaced(inning.balls());
            } else {
                match.setTeamBScore(inning.totalRuns());
                match.setTeamBWickets(inning.wickets());
                match.setTeamBBallsFaced(inning.balls());
            }
        }

        if (winner == null || winner.isBlank()) {
            // No super over played and scores still level => match tied.
            match.setIsMatchTied(true);
            match.setWonBy("Match tied");
            return;
        }

        boolean teamAWon = winner.equalsIgnoreCase(teamAName);
        Team winnerEntity = teamAWon ? teamAEntity : teamBEntity;
        int winningTeamSquadSize = teamAWon ? teamASquadSize : teamBSquadSize;

        match.setWinnerTeam(winnerEntity);

        if (hasSuperOver) {
            match.setWonBy(winner + " won in Super Over");
            return;
        }

        InningsDto winnerInnings = regularInnings.stream()
                .filter(i -> i.battingTeam().equalsIgnoreCase(winner))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No innings found for winning team: " + winner));
        InningsDto loserInnings = regularInnings.stream()
                .filter(i -> !i.battingTeam().equalsIgnoreCase(winner))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No innings found for losing team"));

        boolean winnerBattedFirst = winner.equalsIgnoreCase(battingFirstTeamName);

        if (winnerBattedFirst) {
            int margin = winnerInnings.totalRuns() - loserInnings.totalRuns();
            match.setWinByRuns(margin);
            match.setWonBy(winner + " won by " + margin + " runs");
        } else {
            // Use actual wickets fallen against the full squad size,
            // not battingStats().size() which only counts players who batted
            int wicketsFallen = winnerInnings.wickets();
            int wicketsRemaining = winningTeamSquadSize - wicketsFallen - 1;
            match.setWinByWickets(wicketsRemaining);
            match.setWonBy(winner + " won by " + wicketsRemaining + " wickets");
        }
    }

    /**
     * Test (up to 2 innings per team): flat score fields on Match stay at their
     * defaults (they can't represent multiple innings). One MatchInningsSummary
     * row is created per innings, numbered per-team (1st/2nd time that team
     * batted) so follow-on orderings like A, B, B, A are handled correctly.
     */
    private void applyTestResult(Match match, List<InningsDto> regularInnings,
                                 String teamAName, String teamBName,
                                 Team teamAEntity, Team teamBEntity,
                                 String winner, int teamASquadSize, int teamBSquadSize) {

        Map<String, Integer> teamInningsCounter = new HashMap<>();

        for (InningsDto inning : regularInnings) {
            String battingTeamName = inning.battingTeam();
            Team battingTeamEntity = battingTeamName.equalsIgnoreCase(teamAName) ? teamAEntity : teamBEntity;
            Team bowlingTeamEntity = battingTeamEntity.equals(teamAEntity) ? teamBEntity : teamAEntity;

            int teamInningsNumber = teamInningsCounter.merge(battingTeamName, 1, Integer::sum);

            MatchInningsSummary summary = new MatchInningsSummary();
            summary.setMatch(match);
            summary.setBattingTeam(battingTeamEntity);
            summary.setBowlingTeam(bowlingTeamEntity);
            summary.setTeamInningsNumber(teamInningsNumber);
            summary.setRuns(inning.totalRuns());
            summary.setWickets(inning.wickets());
            summary.setBalls(inning.balls());
            summary.setSuperOver(false);
            summary.setCompleted(true);

            match.getInningsSummaries().add(summary);
        }

        if (winner == null || winner.isBlank()) {
            match.setIsMatchDrawn(true);
            match.setWonBy("Match drawn");
            return;
        }

        boolean teamAWon = winner.equalsIgnoreCase(teamAName);
        Team winnerEntity = teamAWon ? teamAEntity : teamBEntity;
        String winnerTeamName = teamAWon ? teamAName : teamBName;
        String loserTeamName = teamAWon ? teamBName : teamAName;
        int winnerSquadSize = teamAWon ? teamASquadSize : teamBSquadSize;

        match.setWinnerTeam(winnerEntity);

        int winnerInningsCount = teamInningsCounter.getOrDefault(winnerTeamName, 0);
        int loserInningsCount = teamInningsCounter.getOrDefault(loserTeamName, 0);

        int winnerAggregateRuns = regularInnings.stream()
                .filter(i -> i.battingTeam().equalsIgnoreCase(winnerTeamName))
                .mapToInt(InningsDto::totalRuns)
                .sum();
        int loserAggregateRuns = regularInnings.stream()
                .filter(i -> i.battingTeam().equalsIgnoreCase(loserTeamName))
                .mapToInt(InningsDto::totalRuns)
                .sum();

        InningsDto lastInnings = regularInnings.get(regularInnings.size() - 1);
        boolean winnerBattedLast = lastInnings.battingTeam().equalsIgnoreCase(winnerTeamName);

        if (winnerInningsCount < loserInningsCount) {
            // Winner didn't need to bat twice — won by an innings.
            int margin = winnerAggregateRuns - loserAggregateRuns;
            match.setIsInningsWin(true);
            match.setWinByRuns(margin);
            match.setWonBy(winnerTeamName + " won by an innings and " + margin + " runs");
        } else if (winnerBattedLast) {
            // Winner was chasing in the final innings.
            int wicketsFallen = lastInnings.wickets();
            int wicketsRemaining = winnerSquadSize - wicketsFallen - 1;
            match.setWinByWickets(wicketsRemaining);
            match.setWonBy(winnerTeamName + " won by " + wicketsRemaining + " wickets");
        } else {
            // Winner set a target and bowled the opponent out — won by runs.
            int margin = winnerAggregateRuns - loserAggregateRuns;
            match.setWinByRuns(margin);
            match.setWonBy(winnerTeamName + " won by " + margin + " runs");
        }
    }

    // =====================================================================
    // Player name normalization
    // =====================================================================
    // Team names are intentionally left untouched here — resolveTeam/processSingleTeam
    // normalize those themselves. Only the player-name fields are rewritten below, so
    // every player ends up keyed/stored consistently regardless of how the incoming
    // JSON capitalized or spaced their name.

    private MatchDataDto normalizePlayerNames(MatchDataDto dto) {
        Map<String, TeamDto> normalizedTeams = new LinkedHashMap<>();
        if (dto.teams() != null) {
            dto.teams().forEach((key, teamDto) -> normalizedTeams.put(key, normalizeTeamDto(teamDto)));
        }

        List<InningsDto> normalizedInnings = dto.innings() == null
                ? List.of()
                : dto.innings().stream().map(this::normalizeInnings).toList();

        ResultDto normalizedResult = dto.result() == null ? null : new ResultDto(
                dto.result().winner(),
                dto.result().type(),
                dto.result().margin(),
                NameNormalizer.normalize(dto.result().manOfTheMatch())
        );

        return new MatchDataDto(
                dto.seasonId(), normalizedTeams, dto.toss(), dto.rules(), dto.totalOvers(),
                dto.matchType(), normalizedInnings, normalizedResult
        );
    }

    private TeamDto normalizeTeamDto(TeamDto teamDto) {
        if (teamDto == null) {
            return new TeamDto(null, List.of());
        }
        List<String> normalizedPlayers = teamDto.players() == null
                ? List.of()
                : teamDto.players().stream().map(NameNormalizer::normalize).toList();
        return new TeamDto(teamDto.name(), normalizedPlayers);
    }

    private InningsDto normalizeInnings(InningsDto inning) {
        Map<String, BattingStatDto> normalizedBatting = normalizeKeys(inning.battingStats());
        Map<String, BowlingStatDto> normalizedBowling = normalizeKeys(inning.bowlingStats());

        Map<String, DismissalDto> normalizedDismissals = new LinkedHashMap<>();
        if (inning.dismissals() != null) {
            inning.dismissals().forEach((playerName, dismissal) -> normalizedDismissals.put(
                    NameNormalizer.normalize(playerName), normalizeDismissal(dismissal)
            ));
        }

        List<BallDto> normalizedBalls = inning.ballByBall() == null
                ? List.of()
                : inning.ballByBall().stream().map(this::normalizeBall).toList();

        return new InningsDto(
                inning.battingTeam(), inning.bowlingTeam(), inning.totalRuns(), inning.wickets(), inning.balls(),
                normalizedBatting, normalizedBowling, inning.extras(), normalizedDismissals, normalizedBalls,
                inning.isSuperOver(), inning.completed()
        );
    }

    private <T> Map<String, T> normalizeKeys(Map<String, T> map) {
        if (map == null) {
            return Map.of();
        }
        Map<String, T> normalized = new LinkedHashMap<>();
        map.forEach((playerName, value) -> normalized.put(NameNormalizer.normalize(playerName), value));
        return normalized;
    }

    private DismissalDto normalizeDismissal(DismissalDto dismissal) {
        if (dismissal == null) {
            return null;
        }
        return new DismissalDto(
                dismissal.type(),
                NameNormalizer.normalize(dismissal.bowler()),
                NameNormalizer.normalize(dismissal.fielder())
        );
    }

    private BallDto normalizeBall(BallDto ball) {
        return new BallDto(
                ball.over(), ball.ballInOver(), ball.actualBallNum(),
                NameNormalizer.normalize(ball.striker()),
                NameNormalizer.normalize(ball.nonStriker()),
                NameNormalizer.normalize(ball.bowler()),
                ball.runs(), ball.type(), ball.isWicket(),
                normalizeWicket(ball.wicket()),
                ball.timestamp()
        );
    }

    private WicketDto normalizeWicket(WicketDto wicket) {
        if (wicket == null) {
            return null;
        }
        return new WicketDto(
                wicket.type(),
                NameNormalizer.normalize(wicket.outBatsman()),
                NameNormalizer.normalize(wicket.helper())
        );
    }
}