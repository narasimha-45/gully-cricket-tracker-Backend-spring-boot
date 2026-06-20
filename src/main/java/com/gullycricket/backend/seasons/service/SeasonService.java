package com.gullycricket.backend.seasons.service;

import com.gullycricket.backend.matches.respository.MatchRepository;
import com.gullycricket.backend.seasons.DTOs.MatchResponseDto;
import com.gullycricket.backend.seasons.entity.Season;
import com.gullycricket.backend.seasons.repository.SeasonRepository;
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

    public List<MatchResponseDto> getALlMatchesBySeasonId(String seasonId) {
            log.info("Fetching all matches for season with ID {}", seasonId);
            List<MatchResponseDto> matches = matchRepository.findBySeason_Id(seasonId)
                    .stream()
                    .map(this::MatchToResponseDtoMatch)
                    .toList();
            log.info("Retrieved {} matches for season with ID {}", matches.size(), seasonId);
            return matches;
    }

    private MatchResponseDto MatchToResponseDtoMatch(com.gullycricket.backend.matches.entity.Match match) {
        return new MatchResponseDto(
                match.getId(),
                match.getSeason().getId(),
                match.getTeamA(),
                match.getTeamAScore(),
                match.getTeamAWickets(),
                match.getTeamB(),
                match.getTeamBScore(),
                match.getTeamBWickets(),
                match.getWinner(),
                match.getSuperOver(),
                match.getWonBy(),
                match.getCompletedAt()
        );
    }
}
