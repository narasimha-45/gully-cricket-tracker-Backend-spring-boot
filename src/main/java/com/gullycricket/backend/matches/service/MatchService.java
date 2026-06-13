package com.gullycricket.backend.matches.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gullycricket.backend.matches.DTOs.InningsDto;
import com.gullycricket.backend.matches.DTOs.MatchDataDto;
import com.gullycricket.backend.matches.DTOs.MatchResponseDto;
import com.gullycricket.backend.matches.entity.Match;
import com.gullycricket.backend.matches.entity.MatchStatus;
import com.gullycricket.backend.matches.respository.MatchRepository;
import com.gullycricket.backend.seasons.entity.Season;
import com.gullycricket.backend.seasons.repository.SeasonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final SeasonRepository seasonRepository;
    private final ObjectMapper objectMapper;

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

    public MatchResponseDto saveMatch(JsonNode matchData) {
        log.info("Creating new match");

        MatchDataDto dto = objectMapper.convertValue(matchData, MatchDataDto.class);

        Season season = seasonRepository.findById(dto.seasonId())
                .orElseThrow(() -> new RuntimeException("Season not found: " + dto.seasonId()));

        List<InningsDto> regularInnings = dto.innings().stream()
                .filter(i -> !i.isSuperOver())
                .toList();

        boolean hasSuperOver = dto.innings().stream()
                .anyMatch(InningsDto::isSuperOver);

        Match match = new Match();
        match.setSeason(season);
        match.setMatchData(matchData);
        match.setStatus(MatchStatus.COMPLETED);
        match.setTeamA(dto.teams().get("teamA").name());
        match.setTeamB(dto.teams().get("teamB").name());
        match.setTeamAScore(regularInnings.get(0).totalRuns());
        match.setTeamAWickets(regularInnings.get(0).wickets());
        match.setTeamBScore(regularInnings.get(1).totalRuns());
        match.setTeamBWickets(regularInnings.get(1).wickets());
        match.setWinner(dto.result().winner());
        match.setSuperOver(hasSuperOver);
        match.setCompletedAt(LocalDateTime.now());

        Match savedMatch = matchRepository.save(match);
        log.info("Match created with id: {}", savedMatch.getId());

        return new MatchResponseDto(
                savedMatch.getId(),
                savedMatch.getSeason().getId(),
                savedMatch.getMatchData()
        );
    }


}