package com.gullycricket.backend.stats.service;

import com.gullycricket.backend.common.exception.ResourceNotFoundException;
import com.gullycricket.backend.matches.entity.Match;
import com.gullycricket.backend.matches.entity.MatchInningsSummary;
import com.gullycricket.backend.matches.entity.MatchStatus;
import com.gullycricket.backend.matches.repository.MatchRepository;
import com.gullycricket.backend.seasons.entity.Season;
import com.gullycricket.backend.stats.dto.NotableMatchDto;
import com.gullycricket.backend.stats.dto.TeamLeaderboardEntryDto;
import com.gullycricket.backend.stats.dto.TeamProfileDto;
import com.gullycricket.backend.stats.dto.TeamSeasonStatsDto;
import com.gullycricket.backend.stats.enums.MatchResult;
import com.gullycricket.backend.stats.enums.TeamSortBy;
import com.gullycricket.backend.teams.entity.Team;
import com.gullycricket.backend.teams.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamStatsService {

    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;

    // =====================================================================
    // Team profile
    // =====================================================================

    public TeamProfileDto getTeamProfile(String teamId, String seasonId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + teamId));
        String teamName = team.getTeamName();

        List<Match> matches = seasonId != null
                ? matchRepository.findCompletedMatchesForTeamAndSeason(teamId, seasonId, MatchStatus.COMPLETED)
                : matchRepository.findCompletedMatchesForTeam(teamId, MatchStatus.COMPLETED);

        List<NotableMatchDto> notable = matches.stream()
                .map(m -> toNotableMatch(m, teamId))
                .sorted(Comparator.comparing(NotableMatchDto::completedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        TeamRecord record = computeRecord(notable);

        Map<String, List<NotableMatchDto>> bySeasonRaw = notable.stream()
                .collect(Collectors.groupingBy(NotableMatchDto::seasonId));

        List<TeamSeasonStatsDto> bySeason = bySeasonRaw.values().stream()
                .map(this::computeSeasonStats)
                .sorted(Comparator.comparing(TeamSeasonStatsDto::seasonName, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        return new TeamProfileDto(
                teamId, teamName,
                record.matchesPlayed, record.wins, record.losses, record.ties, record.noResults, record.winPercentage(),
                record.battedFirst, record.wonBattingFirst, record.winPercentageBattingFirst(),
                record.battedSecond, record.wonChasing, record.winPercentageChasing(),
                record.highestScore, record.lowestScore, record.lowestDefended, record.highestChased,
                record.totalRunsScored, record.totalRunsConceded,
                record.matchesPlayed == 0 ? 0 : round2((double) record.totalRunsScored / record.matchesPlayed),
                notable.stream().limit(10).toList(),
                bySeason
        );
    }

    private TeamSeasonStatsDto computeSeasonStats(List<NotableMatchDto> seasonMatches) {
        TeamRecord record = computeRecord(seasonMatches);
        NotableMatchDto any = seasonMatches.get(0);

        return new TeamSeasonStatsDto(
                any.seasonId(), any.seasonName(),
                record.matchesPlayed, record.wins, record.losses, record.ties, record.noResults, record.winPercentage(),
                record.battedFirst, record.wonBattingFirst, record.battedSecond, record.wonChasing,
                record.highestScore != null ? record.highestScore.teamScore() : null,
                record.lowestDefended != null ? record.lowestDefended.teamScore() : null,
                record.highestChased != null ? record.highestChased.teamScore() : null,
                record.lowestScore != null ? record.lowestScore.teamScore() : null,
                record.totalRunsScored, record.totalRunsConceded
        );
    }

    // =====================================================================
    // Team leaderboard
    // =====================================================================

    public List<TeamLeaderboardEntryDto> getTeamLeaderboard(String seasonId, TeamSortBy sortBy, Integer limit) {
        List<Team> teams = teamRepository.findAll();

        // ONE query for every completed match (optionally scoped to a season),
        // instead of the previous one-query-per-team loop. Matches are then grouped
        // by team id in memory — this is what keeps the leaderboard fast as the
        // number of teams grows.
        List<Match> allMatches = seasonId != null
                ? matchRepository.findByStatusAndSeasonWithInnings(MatchStatus.COMPLETED, seasonId)
                : matchRepository.findByStatusWithInnings(MatchStatus.COMPLETED);

        Map<String, List<Match>> matchesByTeamId = new HashMap<>();
        for (Match match : allMatches) {
            matchesByTeamId.computeIfAbsent(match.getTeamA().getId(), k -> new ArrayList<>()).add(match);
            matchesByTeamId.computeIfAbsent(match.getTeamB().getId(), k -> new ArrayList<>()).add(match);
        }

        List<TeamLeaderboardEntryDto> entries = teams.stream()
                .map(team -> {
                    List<Match> matches = matchesByTeamId.getOrDefault(team.getId(), List.of());

                    List<NotableMatchDto> notable = matches.stream().map(m -> toNotableMatch(m, team.getId())).toList();
                    TeamRecord record = computeRecord(notable);

                    return new TeamLeaderboardEntryDto(
                            team.getId(), team.getTeamName(),
                            record.matchesPlayed, record.wins, record.losses, record.ties, record.noResults, record.winPercentage(),
                            record.wonBattingFirst, record.wonChasing,
                            record.highestScore != null ? record.highestScore.teamScore() : null,
                            record.lowestDefended != null ? record.lowestDefended.teamScore() : null,
                            record.highestChased != null ? record.highestChased.teamScore() : null,
                            record.totalRunsScored, record.totalRunsConceded
                    );
                })
                .filter(e -> e.matchesPlayed() > 0)
                .collect(Collectors.toCollection(ArrayList::new));

        entries.sort(teamComparator(sortBy));
        int safeLimit = limit == null ? 50 : Math.max(1, Math.min(limit, 100));
        return entries.stream().limit(safeLimit).toList();
    }

    private Comparator<TeamLeaderboardEntryDto> teamComparator(TeamSortBy sortBy) {
        TeamSortBy sort = sortBy != null ? sortBy : TeamSortBy.MATCHES_WON;
        return switch (sort) {
            case WIN_PERCENTAGE -> Comparator.comparing(TeamLeaderboardEntryDto::winPercentage).reversed();
            case HIGHEST_SCORE -> Comparator.comparing(
                    (TeamLeaderboardEntryDto e) -> e.highestTeamScore() == null ? -1 : e.highestTeamScore()
            ).reversed();
            case MATCHES_PLAYED -> Comparator.comparing(TeamLeaderboardEntryDto::matchesPlayed).reversed();
            case CHASES_WON -> Comparator.comparing(TeamLeaderboardEntryDto::timesWonChasing).reversed();
            case DEFENSES_WON -> Comparator.comparing(TeamLeaderboardEntryDto::timesWonBattingFirst).reversed();
            case MATCHES_WON -> Comparator.comparing(TeamLeaderboardEntryDto::matchesWon).reversed();
        };
    }

    // =====================================================================
    // Shared record computation
    // =====================================================================

    private NotableMatchDto toNotableMatch(Match match, String teamId) {
        boolean isTeamA = match.getTeamA().getId().equals(teamId);
        Team opponent = isTeamA ? match.getTeamB() : match.getTeamA();
        TeamScore teamTotals = scoreForTeam(match, isTeamA ? match.getTeamA() : match.getTeamB());
        TeamScore opponentTotals = scoreForTeam(match, opponent);
        int teamScore = teamTotals.runs();
        int teamWickets = teamTotals.wickets();
        int opponentScore = opponentTotals.runs();
        int opponentWickets = opponentTotals.wickets();

        boolean battingFirst = match.getBattingFirstTeam() != null && match.getBattingFirstTeam().getId().equals(teamId);

        MatchResult result;
        if (Boolean.TRUE.equals(match.getIsMatchTied())) {
            result = MatchResult.TIE;
        } else if (Boolean.TRUE.equals(match.getIsMatchDrawn())) {
            result = MatchResult.NO_RESULT;
        } else if (match.getWinnerTeam() == null) {
            result = MatchResult.NO_RESULT;
        } else if (match.getWinnerTeam().getId().equals(teamId)) {
            result = MatchResult.WIN;
        } else {
            result = MatchResult.LOSS;
        }

        Season season = match.getSeason();

        return new NotableMatchDto(
                match.getId(),
                season != null ? season.getId() : null,
                season != null ? season.getSeasonName() : null,
                opponent.getId(), opponent.getTeamName(),
                teamScore, teamWickets, opponentScore, opponentWickets,
                match.getTotalOvers(), battingFirst, result, match.getCompletedAt()
        );
    }

    private TeamRecord computeRecord(List<NotableMatchDto> matches) {
        TeamRecord r = new TeamRecord();
        r.matchesPlayed = matches.size();

        for (NotableMatchDto m : matches) {
            switch (m.result()) {
                case WIN -> r.wins++;
                case LOSS -> r.losses++;
                case TIE -> r.ties++;
                case NO_RESULT -> r.noResults++;
            }

            if (m.battingFirst()) {
                r.battedFirst++;
                if (m.result() == MatchResult.WIN) {
                    r.wonBattingFirst++;
                    if (r.lowestDefended == null || m.teamScore() < r.lowestDefended.teamScore()) {
                        r.lowestDefended = m;
                    }
                }
            } else {
                r.battedSecond++;
                if (m.result() == MatchResult.WIN) {
                    r.wonChasing++;
                    if (r.highestChased == null || m.teamScore() > r.highestChased.teamScore()) {
                        r.highestChased = m;
                    }
                }
            }

            if (r.highestScore == null || m.teamScore() > r.highestScore.teamScore()) {
                r.highestScore = m;
            }
            if (r.lowestScore == null || m.teamScore() < r.lowestScore.teamScore()) {
                r.lowestScore = m;
            }

            r.totalRunsScored += m.teamScore();
            r.totalRunsConceded += m.opponentScore();
        }

        return r;
    }


    private TeamScore scoreForTeam(Match match, Team team) {
        List<MatchInningsSummary> innings = match.getInningsSummaries().stream()
                .filter(i -> !i.isSuperOver() && i.getBattingTeam().getId().equals(team.getId()))
                .toList();
        if (!innings.isEmpty()) {
            return new TeamScore(
                    innings.stream().mapToInt(MatchInningsSummary::getRuns).sum(),
                    innings.stream().mapToInt(MatchInningsSummary::getWickets).sum()
            );
        }
        boolean modelTeamA = match.getTeamA().getId().equals(team.getId());
        return modelTeamA
                ? new TeamScore(nullToZero(match.getTeamAScore()), nullToZero(match.getTeamAWickets()))
                : new TeamScore(nullToZero(match.getTeamBScore()), nullToZero(match.getTeamBWickets()));
    }

    private record TeamScore(int runs, int wickets) {}

    private int nullToZero(Integer value) {
        return value != null ? value : 0;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /**
     * Mutable accumulator used only while folding over a team's matches.
     */
    private static class TeamRecord {
        int matchesPlayed = 0;
        int wins = 0;
        int losses = 0;
        int ties = 0;
        int noResults = 0;

        int battedFirst = 0;
        int wonBattingFirst = 0;
        int battedSecond = 0;
        int wonChasing = 0;

        int totalRunsScored = 0;
        int totalRunsConceded = 0;

        NotableMatchDto highestScore;
        NotableMatchDto lowestScore;
        NotableMatchDto lowestDefended;
        NotableMatchDto highestChased;

        double winPercentage() {
            return matchesPlayed == 0 ? 0 : round2(wins * 100.0 / matchesPlayed);
        }

        double winPercentageBattingFirst() {
            return battedFirst == 0 ? 0 : round2(wonBattingFirst * 100.0 / battedFirst);
        }

        double winPercentageChasing() {
            return battedSecond == 0 ? 0 : round2(wonChasing * 100.0 / battedSecond);
        }
    }
}
