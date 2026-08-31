package com.gullycricket.backend.matches.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gullycricket.backend.common.exception.BadRequestException;
import com.gullycricket.backend.common.exception.ResourceNotFoundException;
import com.gullycricket.backend.common.util.NameNormalizer;
import com.gullycricket.backend.config.CacheNames;
import com.gullycricket.backend.matches.dto.*;
import com.gullycricket.backend.matches.entity.Match;
import com.gullycricket.backend.matches.entity.MatchInningsSummary;
import com.gullycricket.backend.matches.entity.MatchStatus;
import com.gullycricket.backend.matches.entity.MatchType;
import com.gullycricket.backend.matches.repository.MatchRepository;
import com.gullycricket.backend.matches.repository.MatchProjectionMaintenanceRepository;
import com.gullycricket.backend.seasons.entity.Season;
import com.gullycricket.backend.seasons.repository.SeasonRepository;
import com.gullycricket.backend.seasons.service.SeasonService;
import com.gullycricket.backend.teams.entity.Team;
import com.gullycricket.backend.teams.service.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
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
    private final MatchValidator matchValidator;
    private final SeasonService seasonService;
    private final MatchProjectionMaintenanceRepository maintenanceRepository;

    @Transactional(readOnly = true)
    public MatchResponseDto getMatchById(String matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found: " + matchId));
        return toResponse(match);
    }

    /**
     * Backward-compatible entry point used by the Mongo migration.
     */
    @Transactional
    @CacheEvict(value = {
            CacheNames.SEASON_MATCHES,
            CacheNames.BATTING_LEADERBOARD,
            CacheNames.BOWLING_LEADERBOARD,
            CacheNames.FIELDING_LEADERBOARD
    }, allEntries = true)
    public MatchResponseDto saveMatch(JsonNode matchData) {
        try {
            MatchDataDto dto = objectMapper.convertValue(matchData, MatchDataDto.class);
            return saveMatch(dto, null);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid match payload: " + ex.getMessage());
        }
    }

    /**
     * Production API entry point. Idempotency key is optional, but when supplied,
     * retries return the already-created match rather than duplicating it.
     */
    @Transactional
    @CacheEvict(value = {
            CacheNames.SEASON_MATCHES,
            CacheNames.BATTING_LEADERBOARD,
            CacheNames.BOWLING_LEADERBOARD,
            CacheNames.FIELDING_LEADERBOARD
    }, allEntries = true)
    public MatchResponseDto saveMatch(MatchDataDto rawDto, String idempotencyKey) {
        maintenanceRepository.acquireProjectionWriteLock();

        String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);
        if (normalizedIdempotencyKey != null) {
            Match existing = matchRepository.findByIdempotencyKey(normalizedIdempotencyKey).orElse(null);
            if (existing != null) {
                log.info("Returning existing match for idempotency key, matchId={}", existing.getId());
                return toResponse(existing);
            }
        }

        MatchDataDto dto = validateAndNormalize(rawDto);
        Season season = seasonRepository.findById(dto.seasonId())
                .orElseThrow(() -> new ResourceNotFoundException("Season not found: " + dto.seasonId()));

        Match match = new Match();
        applyCanonicalMatchState(match, dto, season);
        match.setCompletedAt(LocalDateTime.now());
        match.setIdempotencyKey(normalizedIdempotencyKey);

        Match savedMatch = matchRepository.save(match);
        processPlayerStatsService.processPlayerStats(savedMatch, dto);

        // Recompute from source rows rather than relying on +1/-1 arithmetic.
        seasonService.syncMatchesPlayed(season.getId());

        long regularInnings = dto.innings().stream().filter(i -> !i.isSuperOver()).count();
        log.info("Match created: matchId={}, seasonId={}, matchType={}, innings={}",
                savedMatch.getId(), season.getId(), dto.matchType(), regularInnings);
        return toResponse(savedMatch);
    }

    /**
     * Replays a persisted match from matches.match_data into the denormalized Match columns,
     * innings summaries and player/stat projections. The caller must delete the old projections
     * first. Package-private by design: MatchRebuildService is the maintenance entry point.
     */
    void replayStoredMatch(Match match) {
        if (match == null || match.getId() == null) {
            throw new BadRequestException("A persisted match is required for replay");
        }
        if (match.getMatchData() == null || match.getMatchData().isNull()) {
            throw new BadRequestException("Match has no stored match_data and cannot be rebuilt: " + match.getId());
        }

        final MatchDataDto rawDto;
        try {
            rawDto = objectMapper.convertValue(match.getMatchData(), MatchDataDto.class);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid stored match_data for match " + match.getId() + ": " + ex.getMessage());
        }

        MatchDataDto dto = validateAndNormalize(rawDto);
        String existingSeasonId = match.getSeason().getId();
        if (!existingSeasonId.equals(dto.seasonId())) {
            throw new BadRequestException(
                    "Stored match_data seasonId does not match matches.season_id for match " + match.getId());
        }

        // Preserve identity, completedAt and idempotencyKey; everything else below is derived
        // again from the canonical JSON payload.
        applyCanonicalMatchState(match, dto, match.getSeason());
        processPlayerStatsService.processPlayerStats(match, dto);
    }

    private MatchDataDto validateAndNormalize(MatchDataDto rawDto) {
        matchValidator.validate(rawDto);
        MatchDataDto dto = normalizeNames(rawDto);
        matchValidator.validate(dto);
        return dto;
    }

    private void applyCanonicalMatchState(Match match, MatchDataDto dto, Season season) {
        List<InningsDto> regularInnings = dto.innings().stream()
                .filter(i -> !i.isSuperOver())
                .toList();
        boolean hasSuperOver = dto.innings().stream().anyMatch(InningsDto::isSuperOver);

        TeamDto teamADto = dto.teams().get("teamA");
        TeamDto teamBDto = dto.teams().get("teamB");
        String teamAName = teamADto.name();
        String teamBName = teamBDto.name();
        String winner = dto.result() == null ? null : dto.result().winner();

        Map<String, Team> existingTeams = new HashMap<>();
        for (Team team : teamService.getTeamsByNames(List.of(teamAName, teamBName))) {
            existingTeams.put(team.getTeamName(), team);
        }
        Team teamAEntity = resolveTeam(teamAName, season, existingTeams.get(teamAName));
        Team teamBEntity = resolveTeam(teamBName, season, existingTeams.get(teamBName));

        String battingFirstTeamName = regularInnings.getFirst().battingTeam();
        boolean teamABattedFirst = battingFirstTeamName.equals(teamAName);
        Team battingFirstTeam = teamABattedFirst ? teamAEntity : teamBEntity;
        Team battingSecondTeam = teamABattedFirst ? teamBEntity : teamAEntity;

        // Reset all derived Match columns first so replay cannot retain stale values from a
        // previous version of the projection logic.
        match.setSeason(season);
        match.setMatchData(objectMapper.valueToTree(dto));
        match.setStatus(MatchStatus.COMPLETED);
        match.setTeamA(teamAEntity);
        match.setTeamB(teamBEntity);
        match.setMatchType(dto.matchType());
        match.setTeamAScore(0);
        match.setTeamAWickets(0);
        match.setTeamABallsFaced(0);
        match.setTeamBScore(0);
        match.setTeamBWickets(0);
        match.setTeamBBallsFaced(0);
        match.setTotalOvers(dto.totalOvers());
        match.setWinnerTeam(null);
        match.setSuperOver(hasSuperOver);
        match.setIsBattingFirstTeamWon(false);
        match.setBattingFirstTeam(battingFirstTeam);
        match.setBattingSecondTeam(battingSecondTeam);
        match.setWinByRuns(null);
        match.setWinByWickets(null);
        match.setIsInningsWin(false);
        match.setIsMatchDrawn(false);
        match.setIsMatchTied(false);
        match.setWonBy(null);
        match.setTestInningsPerTeam(dto.testConfig() == null ? null : dto.testConfig().inningsPerTeam());
        match.setFollowOnEnforced(dto.testConfig() != null && Boolean.TRUE.equals(dto.testConfig().followOnEnforced()));

        if (hasText(winner)) {
            match.setIsBattingFirstTeamWon(winner.equals(battingFirstTeamName));
        }

        match.getInningsSummaries().clear();
        populateInningsSummaries(match, regularInnings, teamAName, teamAEntity, teamBEntity);

        if (dto.matchType() == MatchType.TEST) {
            applyTestResult(match, regularInnings, teamAName, teamBName, teamAEntity, teamBEntity,
                    winner, dto.result() == null ? null : dto.result().type(),
                    teamADto.players().size(), teamBDto.players().size());
        } else {
            applyLimitedOversResult(match, regularInnings, teamAName, teamBName, teamAEntity, teamBEntity,
                    winner, dto.result() == null ? null : dto.result().type(), hasSuperOver,
                    teamADto.players().size(), teamBDto.players().size(), battingFirstTeamName);
        }
    }

    private MatchResponseDto toResponse(Match match) {
        return new MatchResponseDto(match.getId(), match.getSeason().getId(), match.getMatchData());
    }

    private String normalizeIdempotencyKey(String key) {
        if (!hasText(key)) {
            return null;
        }
        String trimmed = key.trim();
        if (trimmed.length() > 128) {
            throw new BadRequestException("Idempotency-Key cannot exceed 128 characters");
        }
        return trimmed;
    }

    private Team resolveTeam(String canonicalTeamName, Season season, Team existingTeam) {
        Team team = existingTeam;
        if (team == null) {
            team = new Team();
            team.setTeamName(canonicalTeamName);
            team = teamService.saveTeam(team);
        }
        // Avoid initializing the Team.seasonsPlayed collection on every match. A direct
        // idempotent join-table insert is one small statement and works for existing teams.
        teamService.ensureTeamInSeason(team.getId(), season.getId());
        return team;
    }

    private void populateInningsSummaries(Match match, List<InningsDto> innings,
                                          String teamAName, Team teamA, Team teamB) {
        Map<String, Integer> perTeamCounter = new HashMap<>();
        for (int i = 0; i < innings.size(); i++) {
            InningsDto inning = innings.get(i);
            Team battingTeam = inning.battingTeam().equals(teamAName) ? teamA : teamB;
            Team bowlingTeam = battingTeam.getId().equals(teamA.getId()) ? teamB : teamA;
            int teamInningsNumber = perTeamCounter.merge(inning.battingTeam(), 1, Integer::sum);

            MatchInningsSummary summary = new MatchInningsSummary();
            summary.setMatch(match);
            summary.setBattingTeam(battingTeam);
            summary.setBowlingTeam(bowlingTeam);
            summary.setSequenceNumber(i + 1);
            summary.setTeamInningsNumber(teamInningsNumber);
            summary.setRuns(inning.totalRuns());
            summary.setWickets(inning.wickets());
            summary.setBalls(inning.balls());
            summary.setSuperOver(false);
            summary.setCompleted(inning.completed());
            summary.setFollowOn(inning.isFollowOn());
            summary.setCompletionReason(inning.completionReason());
            match.getInningsSummaries().add(summary);
        }
    }

    private void applyLimitedOversResult(Match match, List<InningsDto> regularInnings,
                                         String teamAName, String teamBName,
                                         Team teamAEntity, Team teamBEntity,
                                         String winner, String resultType, boolean hasSuperOver,
                                         int teamASquadSize, int teamBSquadSize,
                                         String battingFirstTeamName) {
        for (InningsDto inning : regularInnings) {
            if (inning.battingTeam().equals(teamAName)) {
                match.setTeamAScore(inning.totalRuns());
                match.setTeamAWickets(inning.wickets());
                match.setTeamABallsFaced(inning.balls());
            } else if (inning.battingTeam().equals(teamBName)) {
                match.setTeamBScore(inning.totalRuns());
                match.setTeamBWickets(inning.wickets());
                match.setTeamBBallsFaced(inning.balls());
            }
        }

        if (!hasText(winner)) {
            if (isNoResult(resultType)) {
                match.setWonBy("No result");
            } else {
                match.setIsMatchTied(true);
                match.setWonBy("Match tied");
            }
            return;
        }

        boolean teamAWon = winner.equals(teamAName);
        Team winnerEntity = teamAWon ? teamAEntity : teamBEntity;
        int winningTeamSquadSize = teamAWon ? teamASquadSize : teamBSquadSize;
        match.setWinnerTeam(winnerEntity);

        if (hasSuperOver) {
            match.setWonBy(winner + " won in Super Over");
            return;
        }

        InningsDto winnerInnings = regularInnings.stream()
                .filter(i -> i.battingTeam().equals(winner))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No regular innings found for winning team: " + winner));
        InningsDto loserInnings = regularInnings.stream()
                .filter(i -> !i.battingTeam().equals(winner))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No regular innings found for losing team"));

        if (winner.equals(battingFirstTeamName)) {
            int margin = winnerInnings.totalRuns() - loserInnings.totalRuns();
            match.setWinByRuns(Math.max(0, margin));
            match.setWonBy(winner + " won by " + Math.max(0, margin) + " runs");
        } else {
            int wicketsRemaining = Math.max(0, (winningTeamSquadSize - 1) - winnerInnings.wickets());
            match.setWinByWickets(wicketsRemaining);
            match.setWonBy(winner + " won by " + wicketsRemaining + " wickets");
        }
    }

    private void applyTestResult(Match match, List<InningsDto> regularInnings,
                                 String teamAName, String teamBName,
                                 Team teamAEntity, Team teamBEntity,
                                 String winner, String resultType, int teamASquadSize, int teamBSquadSize) {
        if (!hasText(winner)) {
            if (isNoResult(resultType)) {
                match.setWonBy("No result");
            } else {
                match.setIsMatchDrawn(true);
                match.setWonBy("Match drawn");
            }
            return;
        }

        boolean teamAWon = winner.equals(teamAName);
        Team winnerEntity = teamAWon ? teamAEntity : teamBEntity;
        String winnerTeamName = teamAWon ? teamAName : teamBName;
        String loserTeamName = teamAWon ? teamBName : teamAName;
        int winnerSquadSize = teamAWon ? teamASquadSize : teamBSquadSize;
        match.setWinnerTeam(winnerEntity);

        long winnerInningsCount = regularInnings.stream().filter(i -> i.battingTeam().equals(winnerTeamName)).count();
        long loserInningsCount = regularInnings.stream().filter(i -> i.battingTeam().equals(loserTeamName)).count();

        int winnerAggregateRuns = aggregateRuns(regularInnings, winnerTeamName);
        int loserAggregateRuns = aggregateRuns(regularInnings, loserTeamName);

        InningsDto lastInnings = regularInnings.getLast();
        boolean winnerBattedLast = lastInnings.battingTeam().equals(winnerTeamName);

        if (winnerInningsCount < loserInningsCount) {
            int margin = Math.max(0, winnerAggregateRuns - loserAggregateRuns);
            match.setIsInningsWin(true);
            match.setWinByRuns(margin);
            match.setWonBy(winnerTeamName + " won by an innings and " + margin + " runs");
        } else if (winnerBattedLast) {
            int wicketsRemaining = Math.max(0, (winnerSquadSize - 1) - lastInnings.wickets());
            match.setWinByWickets(wicketsRemaining);
            match.setWonBy(winnerTeamName + " won by " + wicketsRemaining + " wickets");
        } else {
            int margin = Math.max(0, winnerAggregateRuns - loserAggregateRuns);
            match.setWinByRuns(margin);
            match.setWonBy(winnerTeamName + " won by " + margin + " runs");
        }
    }

    private int aggregateRuns(List<InningsDto> innings, String teamName) {
        return innings.stream()
                .filter(i -> i.battingTeam().equals(teamName))
                .mapToInt(InningsDto::totalRuns)
                .sum();
    }

    // =====================================================================
    // Canonicalization
    // =====================================================================

    private MatchDataDto normalizeNames(MatchDataDto dto) {
        Map<String, TeamDto> normalizedTeams = new LinkedHashMap<>();
        dto.teams().forEach((key, teamDto) -> normalizedTeams.put(key, normalizeTeamDto(teamDto)));

        List<InningsDto> normalizedInnings = dto.innings().stream().map(this::normalizeInnings).toList();

        ResultDto normalizedResult = dto.result() == null ? null : new ResultDto(
                normalize(dto.result().winner()),
                dto.result().type(),
                dto.result().margin(),
                normalize(dto.result().manOfTheMatch())
        );

        TossDto normalizedToss = dto.toss() == null ? null : new TossDto(
                normalize(dto.toss().winner()),
                dto.toss().decision()
        );

        return new MatchDataDto(
                dto.seasonId(), normalizedTeams, normalizedToss, normalizeRules(dto.rules()), dto.totalOvers(),
                dto.matchType(), dto.testConfig(), normalizedInnings, normalizedResult
        );
    }

    private RulesDto normalizeRules(RulesDto rules) {
        if (rules != null) {
            return rules;
        }
        RuleDetailDto noExtra = new RuleDetailDto(false, false);
        return new RulesDto(noExtra, noExtra);
    }

    private TeamDto normalizeTeamDto(TeamDto teamDto) {
        List<String> players = teamDto.players().stream().map(this::normalize).toList();
        return new TeamDto(normalize(teamDto.name()), players);
    }

    private InningsDto normalizeInnings(InningsDto inning) {
        Map<String, BattingStatDto> batting = normalizeKeys(inning.battingStats());
        Map<String, BowlingStatDto> bowling = normalizeKeys(inning.bowlingStats());

        Map<String, DismissalDto> dismissals = new LinkedHashMap<>();
        if (inning.dismissals() != null) {
            inning.dismissals().forEach((player, dismissal) -> dismissals.put(
                    normalize(player), normalizeDismissal(dismissal)));
        }

        List<BallDto> balls = inning.ballByBall() == null
                ? List.of()
                : inning.ballByBall().stream().map(this::normalizeBall).toList();

        return new InningsDto(
                normalize(inning.battingTeam()), normalize(inning.bowlingTeam()), inning.inningsNumber(),
                inning.totalRuns(), inning.wickets(), inning.balls(), batting, bowling,
                inning.extras(), dismissals, balls, inning.isSuperOver(), inning.isFollowOn(),
                inning.completed(), inning.completionReason()
        );
    }

    private <T> Map<String, T> normalizeKeys(Map<String, T> map) {
        if (map == null) {
            return Map.of();
        }
        Map<String, T> normalized = new LinkedHashMap<>();
        map.forEach((key, value) -> normalized.put(normalize(key), value));
        return normalized;
    }

    private DismissalDto normalizeDismissal(DismissalDto dismissal) {
        if (dismissal == null) {
            return null;
        }
        return new DismissalDto(dismissal.type(), normalize(dismissal.bowler()), normalize(dismissal.fielder()));
    }

    private BallDto normalizeBall(BallDto ball) {
        if (ball == null) {
            return null;
        }
        return new BallDto(
                ball.over(), ball.ballInOver(), ball.actualBallNum(), normalize(ball.striker()),
                normalize(ball.nonStriker()), normalize(ball.bowler()), ball.runs(), ball.type(),
                ball.isWicket(), normalizeWicket(ball.wicket()), ball.timestamp()
        );
    }

    private WicketDto normalizeWicket(WicketDto wicket) {
        if (wicket == null) {
            return null;
        }
        return new WicketDto(wicket.type(), normalize(wicket.outBatsman()), normalize(wicket.helper()));
    }

    private String normalize(String value) {
        return NameNormalizer.normalize(value);
    }

    private boolean isNoResult(String resultType) {
        if (!hasText(resultType)) {
            return false;
        }
        String normalized = resultType.trim().replace('-', '_').replace(' ', '_').toUpperCase();
        return normalized.equals("NO_RESULT") || normalized.equals("ABANDONED") || normalized.equals("CANCELLED");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
