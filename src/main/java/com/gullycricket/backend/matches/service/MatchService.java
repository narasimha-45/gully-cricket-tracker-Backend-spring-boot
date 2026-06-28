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
    private final ProcessPlayerStatsService processPlayerStatsService;

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

        String teamAName = dto.teams().get("teamA").name();
        String teamBName = dto.teams().get("teamB").name();
        String winner = dto.result().winner();

        InningsDto teamAInnings = regularInnings.get(0);
        InningsDto teamBInnings = regularInnings.get(1);

        InningsDto winnerInnings = winner.equalsIgnoreCase(teamAName) ? teamAInnings : teamBInnings;
        InningsDto loserInnings  = winner.equalsIgnoreCase(teamAName) ? teamBInnings : teamAInnings;

        String wonBy = calculateWonBy(winner, winnerInnings, loserInnings, hasSuperOver);

        Match match = new Match();
        match.setSeason(season);
        match.setMatchData(matchData);
        match.setStatus(MatchStatus.COMPLETED);
        match.setTeamA(teamAName);
        match.setTeamB(teamBName);
        match.setTeamAScore(teamAInnings.totalRuns());
        match.setTeamAWickets(teamAInnings.wickets());
        match.setTeamABallsFaced(teamAInnings.balls());
        match.setTeamBScore(teamBInnings.totalRuns());
        match.setTeamBWickets(teamBInnings.wickets());
        match.setTeamBBallsFaced(teamBInnings.balls());
        match.setWinner(winner);
        match.setMatchFormat(dto.matchFormat());
        match.setTotalOvers(dto.totalOvers());
        match.setSuperOver(hasSuperOver);
        match.setWonBy(wonBy);
        match.setCompletedAt(LocalDateTime.now());


        Match savedMatch = matchRepository.save(match);

        log.info("Match created with id: {}", savedMatch.getId());

        processPlayerStatsService.processPlayerStats(savedMatch,dto);
        return new MatchResponseDto(
                savedMatch.getId(),
                savedMatch.getSeason().getId(),
                savedMatch.getMatchData()
        );
    }

    private String calculateWonBy(String winner,
                                  InningsDto winnerInnings, InningsDto loserInnings,
                                  boolean hasSuperOver) {
        if (hasSuperOver) {
            return winner + " won in Super Over";
        }

        int winnerRuns = winnerInnings.totalRuns();
        int loserRuns = loserInnings.totalRuns();

        if (winnerRuns > loserRuns) {
            // Winner batted first — won by runs
            int margin = winnerRuns - loserRuns;
            return winner + " won by " + margin + " runs";
        } else {
            // Winner batted second — won by wickets
            // Use actual wickets fallen, not 10 - wickets
            int wicketsFallen = winnerInnings.wickets();
            int totalPlayers = winnerInnings.battingStats().size();
            int wicketsRemaining = totalPlayers - wicketsFallen - 1;
            return winner + " won by " + wicketsRemaining + " wickets";
        }
    }



}