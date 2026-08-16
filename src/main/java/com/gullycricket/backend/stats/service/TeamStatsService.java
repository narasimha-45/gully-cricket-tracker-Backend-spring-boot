package com.gullycricket.backend.stats.service;

import com.gullycricket.backend.common.exception.ResourceNotFoundException;
import com.gullycricket.backend.matches.repository.read.MatchSummaryReadRepository;
import com.gullycricket.backend.matches.repository.read.MatchSummaryRow;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamStatsService {

    private final MatchSummaryReadRepository matchSummaryReadRepository;
    private final TeamRepository teamRepository;

    public TeamProfileDto getTeamProfile(String teamId, String seasonId) {
        List<MatchSummaryRow> rows = matchSummaryReadRepository.findCompletedForTeam(teamId, seasonId);
        String teamName;
        if (rows.isEmpty()) {
            // Only pay the existence lookup for a team with no completed matches.
            Team team = teamRepository.findById(teamId)
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + teamId));
            teamName = team.getTeamName();
        } else {
            MatchSummaryRow first = rows.getFirst();
            teamName = first.teamAId().equals(teamId) ? first.teamAName() : first.teamBName();
        }

        List<NotableMatchDto> notable = rows.stream()
                .map(row -> toNotableMatch(row, teamId))
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

    public List<TeamLeaderboardEntryDto> getTeamLeaderboard(String seasonId, TeamSortBy sortBy, Integer limit) {
        List<MatchSummaryRow> allMatches = matchSummaryReadRepository.findCompleted(seasonId);
        Map<String, List<MatchSummaryRow>> matchesByTeamId = new HashMap<>();
        Map<String, String> teamNames = new HashMap<>();

        for (MatchSummaryRow match : allMatches) {
            matchesByTeamId.computeIfAbsent(match.teamAId(), ignored -> new ArrayList<>()).add(match);
            matchesByTeamId.computeIfAbsent(match.teamBId(), ignored -> new ArrayList<>()).add(match);
            teamNames.put(match.teamAId(), match.teamAName());
            teamNames.put(match.teamBId(), match.teamBName());
        }

        List<TeamLeaderboardEntryDto> entries = matchesByTeamId.entrySet().stream()
                .map(entry -> {
                    String teamId = entry.getKey();
                    List<NotableMatchDto> notable = entry.getValue().stream()
                            .map(row -> toNotableMatch(row, teamId))
                            .toList();
                    TeamRecord record = computeRecord(notable);
                    return new TeamLeaderboardEntryDto(
                            teamId, teamNames.get(teamId),
                            record.matchesPlayed, record.wins, record.losses, record.ties, record.noResults, record.winPercentage(),
                            record.wonBattingFirst, record.wonChasing,
                            record.highestScore != null ? record.highestScore.teamScore() : null,
                            record.lowestDefended != null ? record.lowestDefended.teamScore() : null,
                            record.highestChased != null ? record.highestChased.teamScore() : null,
                            record.totalRunsScored, record.totalRunsConceded
                    );
                })
                .collect(Collectors.toCollection(ArrayList::new));

        entries.sort(teamComparator(sortBy));
        int safeLimit = limit == null ? 50 : Math.max(1, Math.min(limit, 100));
        return entries.stream().limit(safeLimit).toList();
    }

    private TeamSeasonStatsDto computeSeasonStats(List<NotableMatchDto> seasonMatches) {
        TeamRecord record = computeRecord(seasonMatches);
        NotableMatchDto any = seasonMatches.getFirst();
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

    private NotableMatchDto toNotableMatch(MatchSummaryRow match, String teamId) {
        boolean isTeamA = match.teamAId().equals(teamId);
        String opponentId = isTeamA ? match.teamBId() : match.teamAId();
        String opponentName = isTeamA ? match.teamBName() : match.teamAName();
        int teamScore = isTeamA ? match.teamARuns() : match.teamBRuns();
        int teamWickets = isTeamA ? match.teamAWickets() : match.teamBWickets();
        int opponentScore = isTeamA ? match.teamBRuns() : match.teamARuns();
        int opponentWickets = isTeamA ? match.teamBWickets() : match.teamAWickets();
        boolean battingFirst = teamId.equals(match.battingFirstTeamId());

        MatchResult result;
        if (match.matchTied()) {
            result = MatchResult.TIE;
        } else if (match.matchDrawn() || match.winnerTeamId() == null) {
            result = MatchResult.NO_RESULT;
        } else if (teamId.equals(match.winnerTeamId())) {
            result = MatchResult.WIN;
        } else {
            result = MatchResult.LOSS;
        }

        return new NotableMatchDto(
                match.matchId(), match.seasonId(), match.seasonName(),
                opponentId, opponentName,
                teamScore, teamWickets, opponentScore, opponentWickets,
                match.totalOvers(), battingFirst, result, match.completedAt()
        );
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

            if (r.highestScore == null || m.teamScore() > r.highestScore.teamScore()) r.highestScore = m;
            if (r.lowestScore == null || m.teamScore() < r.lowestScore.teamScore()) r.lowestScore = m;
            r.totalRunsScored += m.teamScore();
            r.totalRunsConceded += m.opponentScore();
        }
        return r;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static class TeamRecord {
        int matchesPlayed;
        int wins;
        int losses;
        int ties;
        int noResults;
        int battedFirst;
        int wonBattingFirst;
        int battedSecond;
        int wonChasing;
        int totalRunsScored;
        int totalRunsConceded;
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
