package com.gullycricket.backend.seasons.service;

import com.gullycricket.backend.common.exception.BadRequestException;
import com.gullycricket.backend.common.exception.ResourceNotFoundException;
import com.gullycricket.backend.config.CacheNames;
import com.gullycricket.backend.matches.entity.MatchType;
import com.gullycricket.backend.matches.repository.read.MatchInningsSummaryRow;
import com.gullycricket.backend.matches.repository.read.MatchSummaryReadRepository;
import com.gullycricket.backend.matches.repository.read.MatchSummaryRow;
import com.gullycricket.backend.seasons.dto.MatchInningsSummaryDto;
import com.gullycricket.backend.seasons.dto.MatchResponseDto;
import com.gullycricket.backend.seasons.dto.SeasonSearchDto;
import com.gullycricket.backend.seasons.entity.Season;
import com.gullycricket.backend.seasons.repository.SeasonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeasonService {

    private final SeasonRepository seasonRepository;
    private final MatchSummaryReadRepository matchSummaryReadRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.ALL_SEASONS, sync = true)
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

    @CacheEvict(value = CacheNames.ALL_SEASONS, allEntries = true)
    public Season createSeason(String seasonName) {
        if (seasonName == null || seasonName.isBlank()) {
            throw new BadRequestException("Season name is required");
        }
        Season season = new Season();
        season.setSeasonName(seasonName.trim());
        season.setCreatedAt(LocalDateTime.now());
        return seasonRepository.save(season);
    }

    @CacheEvict(value = CacheNames.ALL_SEASONS, allEntries = true)
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


    @Transactional
    @CacheEvict(value = CacheNames.ALL_SEASONS, allEntries = true)
    public void incrementMatchesPlayed(String seasonId) {
        int updated = seasonRepository.incrementMatchesPlayed(seasonId);
        if (updated == 0) {
            throw new ResourceNotFoundException("Season not found with ID: " + seasonId);
        }
    }

    @Cacheable(value = CacheNames.SEASON_MATCHES, key = "#seasonId", sync = true)
    public List<MatchResponseDto> getAllMatchesBySeasonId(String seasonId) {
        List<MatchSummaryRow> matches = matchSummaryReadRepository.findBySeasonId(seasonId);
        // Active seasons normally have matches, so avoid paying an extra EXISTS round trip
        // on every request. We only verify the season when the read model is empty.
        if (matches.isEmpty() && !seasonRepository.existsById(seasonId)) {
            throw new ResourceNotFoundException("Season not found with ID: " + seasonId);
        }
        Map<String, List<MatchInningsSummaryDto>> inningsByMatchId = loadInningsByMatchId(matches);
        return matches.stream()
                .map(match -> matchToResponseDtoMatch(
                        match,
                        inningsByMatchId.getOrDefault(match.matchId(), List.of())
                ))
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

    private MatchResponseDto matchToResponseDtoMatch(
            MatchSummaryRow match,
            List<MatchInningsSummaryDto> innings
    ) {
        boolean teamAIsBattingFirst = match.battingFirstTeamId() != null
                && match.battingFirstTeamId().equals(match.teamAId());

        String displayTeamAName = teamAIsBattingFirst ? match.teamAName() : match.teamBName();
        int displayTeamARuns = teamAIsBattingFirst ? match.teamARuns() : match.teamBRuns();
        int displayTeamAWickets = teamAIsBattingFirst ? match.teamAWickets() : match.teamBWickets();
        int displayTeamABalls = teamAIsBattingFirst ? match.teamABalls() : match.teamBBalls();

        String displayTeamBName = teamAIsBattingFirst ? match.teamBName() : match.teamAName();
        int displayTeamBRuns = teamAIsBattingFirst ? match.teamBRuns() : match.teamARuns();
        int displayTeamBWickets = teamAIsBattingFirst ? match.teamBWickets() : match.teamAWickets();
        int displayTeamBBalls = teamAIsBattingFirst ? match.teamBBalls() : match.teamABalls();

        return new MatchResponseDto(
                match.matchId(),
                match.seasonId(),
                displayTeamAName,
                displayTeamARuns,
                displayTeamAWickets,
                displayTeamBName,
                displayTeamBRuns,
                displayTeamBWickets,
                match.winnerTeamName(),
                match.superOver(),
                match.wonBy(),
                match.completedAt(),
                match.status(),
                match.matchType(),
                displayTeamABalls,
                displayTeamBBalls,
                match.totalOvers(),
                innings
        );
    }

    private Map<String, List<MatchInningsSummaryDto>> loadInningsByMatchId(List<MatchSummaryRow> matches) {
        if (matches.isEmpty()) {
            return Map.of();
        }

        List<String> matchIds = matches.stream()
                .filter(match -> match.matchType() == MatchType.TEST)
                .map(MatchSummaryRow::matchId)
                .toList();
        if (matchIds.isEmpty()) {
            return Map.of();
        }

        List<MatchInningsSummaryRow> rows = matchSummaryReadRepository.findInningsByMatchIds(matchIds);
        Map<String, List<MatchInningsSummaryDto>> grouped = new LinkedHashMap<>();

        for (MatchInningsSummaryRow row : rows) {
            grouped.computeIfAbsent(row.matchId(), ignored -> new java.util.ArrayList<>())
                    .add(new MatchInningsSummaryDto(
                            row.sequenceNumber(),
                            row.teamInningsNumber(),
                            row.battingTeamName(),
                            row.runs(),
                            row.wickets(),
                            row.balls(),
                            row.superOver(),
                            row.completed()
                    ));
        }
        return grouped;
    }

}
