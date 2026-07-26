package com.gullycricket.backend.seasons.service;

import com.gullycricket.backend.matches.entity.Match;
import com.gullycricket.backend.matches.respository.MatchRepository;
import com.gullycricket.backend.seasons.DTOs.MatchResponseDto;
import com.gullycricket.backend.seasons.DTOs.SeasonSearchDto;
import com.gullycricket.backend.seasons.entity.Season;
import com.gullycricket.backend.seasons.repository.SeasonRepository;
import com.gullycricket.backend.teams.entity.Team;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class SeasonService {

    @Autowired
    private SeasonRepository seasonJpaRepository;

    @Autowired
    private MatchRepository matchRepository;

    public List<Season> getAllSeasons(){
        log.info("Fetching all seasons from database");
        List<Season> fetchedSeasons = seasonJpaRepository.findAll();
        log.info("Retrieved {} seasons from database", fetchedSeasons.size());
        return fetchedSeasons;
    }

    public Season getSeasonById(String seasonId){
        log.info("Fetching season with ID {}", seasonId);
        Season season = seasonJpaRepository.findById(seasonId)
                .orElseThrow(() -> new RuntimeException("Season not found with ID: " + seasonId));
        log.info("Retrieved season with ID {} got season name: {}", seasonId, season.getSeasonName());
        return season;
    }

    public Season createSeason(String seasonName){
        log.info("Creating new season with name {}", seasonName);
        Season season = new Season();
        season.setSeasonName(seasonName);
        season.setCreatedAt(LocalDateTime.now());
        Season createdSeason = seasonJpaRepository.save(season);
        log.info("Season created with ID {}", createdSeason.getId());
        return createdSeason;
    }

    public Season updateSeason(Season season) {
        log.info("Updating season with ID {} to new name {}", season.getId(), season.getSeasonName());
        Season updatedSeason = seasonJpaRepository.save(season);
        log.info("Season with ID {} updated to new name {}", season.getId(), updatedSeason.getSeasonName());
        return updatedSeason;
    }

    public List<MatchResponseDto> getALlMatchesBySeasonId(String seasonId) {
        log.info("Fetching all matches for season with ID {}", seasonId);
        List<MatchResponseDto> matches = matchRepository.findBySeason_Id(seasonId)
                .stream()
                .map(this::matchToResponseDtoMatch)
                .toList();
        log.info("Retrieved {} matches for season with ID {}", matches.size(), seasonId);
        return matches;
    }

    public List<SeasonSearchDto> searchSeasons(String query){
        log.info("Searching for players like {}",query);

        List<SeasonSearchDto> seasons = seasonJpaRepository.findBySeasonNameContainingIgnoreCase(query)
                .stream()
                .map(this::matchToSeasonSearchDto)
                .toList();

        log.info("List of seasons received: {}",seasons);

        return seasons;
    }

    private SeasonSearchDto matchToSeasonSearchDto(Season season){
        return new SeasonSearchDto(
                season.getId(),
                season.getSeasonName(),
                season.getMatchesPlayed()
        );
    }

    private MatchResponseDto matchToResponseDtoMatch(Match match) {
        // teamA in the response always means "the team that batted first" —
        // not necessarily match.getTeamA(). Swap teamA/teamB (and their
        // scores/wickets/balls) if match.getTeamA() actually batted second.
        Team battingFirstTeam = match.getBattingFirstTeam();
        boolean teamAIsBattingFirst = battingFirstTeam != null && battingFirstTeam.equals(match.getTeamA());

        Team displayTeamA = teamAIsBattingFirst ? match.getTeamA() : match.getTeamB();
        Team displayTeamB = teamAIsBattingFirst ? match.getTeamB() : match.getTeamA();

        Integer displayTeamAScore = teamAIsBattingFirst ? match.getTeamAScore() : match.getTeamBScore();
        Integer displayTeamAWickets = teamAIsBattingFirst ? match.getTeamAWickets() : match.getTeamBWickets();
        Integer displayTeamABallsFaced = teamAIsBattingFirst ? match.getTeamABallsFaced() : match.getTeamBBallsFaced();

        Integer displayTeamBScore = teamAIsBattingFirst ? match.getTeamBScore() : match.getTeamAScore();
        Integer displayTeamBWickets = teamAIsBattingFirst ? match.getTeamBWickets() : match.getTeamAWickets();
        Integer displayTeamBBallsFaced = teamAIsBattingFirst ? match.getTeamBBallsFaced() : match.getTeamABallsFaced();

        // winnerTeam is null for a draw or tie — guard against NPE rather than
        // assuming every match has a winner.
        String winnerTeamName = match.getWinnerTeam() != null ? match.getWinnerTeam().getTeamName() : null;

        return new MatchResponseDto(
                match.getId(),
                match.getSeason().getId(),
                displayTeamA.getTeamName(),
                displayTeamAScore,
                displayTeamAWickets,
                displayTeamB.getTeamName(),
                displayTeamBScore,
                displayTeamBWickets,
                winnerTeamName,
                match.getSuperOver(),
                match.getWonBy(),
                match.getCompletedAt(),
                match.getStatus(),
                displayTeamABallsFaced,
                displayTeamBBallsFaced,
                match.getTotalOvers()
        );
    }
}