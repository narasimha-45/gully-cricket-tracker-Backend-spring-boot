package com.gullycricket.backend.seasons.service;

import com.gullycricket.backend.common.exception.BadRequestException;
import com.gullycricket.backend.common.exception.ResourceNotFoundException;
import com.gullycricket.backend.matches.entity.Match;
import com.gullycricket.backend.matches.entity.MatchInningsSummary;
import com.gullycricket.backend.matches.repository.MatchRepository;
import com.gullycricket.backend.seasons.dto.MatchResponseDto;
import com.gullycricket.backend.seasons.dto.SeasonSearchDto;
import com.gullycricket.backend.seasons.entity.Season;
import com.gullycricket.backend.seasons.repository.SeasonRepository;
import com.gullycricket.backend.teams.entity.Team;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeasonService {

    private final SeasonRepository seasonRepository;
    private final MatchRepository matchRepository;

    @Transactional(readOnly = true)
    public List<Season> getAllSeasons() {
        return seasonRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Season getSeasonById(String seasonId) {
        return seasonRepository.findById(seasonId)
                .orElseThrow(() -> new ResourceNotFoundException("Season not found with ID: " + seasonId));
    }


    @Transactional(readOnly = true)
    public Season findSeasonByName(String seasonName) {
        if (seasonName == null || seasonName.isBlank()) {
            return null;
        }
        return seasonRepository.findFirstBySeasonNameIgnoreCase(seasonName.trim()).orElse(null);
    }

    public Season createSeason(String seasonName) {
        if (seasonName == null || seasonName.isBlank()) {
            throw new BadRequestException("Season name is required");
        }
        Season season = new Season();
        season.setSeasonName(seasonName.trim());
        season.setCreatedAt(LocalDateTime.now());
        return seasonRepository.save(season);
    }

    public Season updateSeason(Season season) {
        if (season == null || season.getId() == null) {
            throw new BadRequestException("Season id is required");
        }
        if (!seasonRepository.existsById(season.getId())) {
            throw new ResourceNotFoundException("Season not found with ID: " + season.getId());
        }
        if (season.getSeasonName() == null || season.getSeasonName().isBlank()) {
            throw new BadRequestException("Season name is required");
        }
        season.setSeasonName(season.getSeasonName().trim());
        return seasonRepository.save(season);
    }

    @Transactional(readOnly = true)
    public List<MatchResponseDto> getAllMatchesBySeasonId(String seasonId) {
        if (!seasonRepository.existsById(seasonId)) {
            throw new ResourceNotFoundException("Season not found with ID: " + seasonId);
        }
        return matchRepository.findBySeason_Id(seasonId).stream()
                .map(this::matchToResponseDtoMatch)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SeasonSearchDto> searchSeasons(String query) {
        String normalized = query == null ? "" : query.trim();
        if (normalized.length() < 2) {
            return List.of();
        }
        return seasonRepository.findTop10BySeasonNameContainingIgnoreCaseOrderBySeasonNameAsc(normalized).stream()
                .map(this::matchToSeasonSearchDto)
                .toList();
    }

    private SeasonSearchDto matchToSeasonSearchDto(Season season) {
        return new SeasonSearchDto(season.getId(), season.getSeasonName(), season.getMatchesPlayed());
    }

    private MatchResponseDto matchToResponseDtoMatch(Match match) {
        Team battingFirstTeam = match.getBattingFirstTeam();
        boolean modelTeamAIsBattingFirst = battingFirstTeam != null && battingFirstTeam.equals(match.getTeamA());

        Team displayTeamA = modelTeamAIsBattingFirst ? match.getTeamA() : match.getTeamB();
        Team displayTeamB = modelTeamAIsBattingFirst ? match.getTeamB() : match.getTeamA();

        TeamAggregate first = aggregateForTeam(match, displayTeamA);
        TeamAggregate second = aggregateForTeam(match, displayTeamB);

        String winnerTeamName = match.getWinnerTeam() != null ? match.getWinnerTeam().getTeamName() : null;

        return new MatchResponseDto(
                match.getId(),
                match.getSeason().getId(),
                displayTeamA.getTeamName(),
                first.runs(),
                first.wickets(),
                displayTeamB.getTeamName(),
                second.runs(),
                second.wickets(),
                winnerTeamName,
                match.getSuperOver(),
                match.getWonBy(),
                match.getCompletedAt(),
                match.getStatus(),
                first.balls(),
                second.balls(),
                match.getTotalOvers()
        );
    }

    /**
     * MatchInningsSummary is the canonical score model. Fallback to legacy flat
     * fields keeps previously saved limited-over matches readable.
     */
    private TeamAggregate aggregateForTeam(Match match, Team team) {
        List<MatchInningsSummary> summaries = match.getInningsSummaries().stream()
                .filter(i -> !i.isSuperOver() && i.getBattingTeam().getId().equals(team.getId()))
                .toList();

        if (!summaries.isEmpty()) {
            return new TeamAggregate(
                    summaries.stream().mapToInt(MatchInningsSummary::getRuns).sum(),
                    summaries.stream().mapToInt(MatchInningsSummary::getWickets).sum(),
                    summaries.stream().mapToInt(MatchInningsSummary::getBalls).sum()
            );
        }

        boolean isModelTeamA = match.getTeamA().getId().equals(team.getId());
        return isModelTeamA
                ? new TeamAggregate(nvl(match.getTeamAScore()), nvl(match.getTeamAWickets()), nvl(match.getTeamABallsFaced()))
                : new TeamAggregate(nvl(match.getTeamBScore()), nvl(match.getTeamBWickets()), nvl(match.getTeamBBallsFaced()));
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private record TeamAggregate(int runs, int wickets, int balls) {}
}
