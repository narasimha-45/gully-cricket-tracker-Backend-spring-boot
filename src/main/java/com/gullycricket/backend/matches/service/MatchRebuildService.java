package com.gullycricket.backend.matches.service;

import com.gullycricket.backend.common.exception.ResourceNotFoundException;
import com.gullycricket.backend.config.CacheNames;
import com.gullycricket.backend.matches.dto.RebuildResultDto;
import com.gullycricket.backend.matches.entity.Match;
import com.gullycricket.backend.matches.repository.MatchProjectionMaintenanceRepository;
import com.gullycricket.backend.matches.repository.MatchRepository;
import com.gullycricket.backend.seasons.repository.SeasonRepository;
import com.gullycricket.backend.seasons.service.SeasonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Rebuilds all match-derived projections from matches.match_data.
 *
 * <p>Source-of-truth tables are preserved: matches, seasons, players and teams.
 * Season/player/team association tables are treated as projections during a season/full rebuild.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchRebuildService {

    private final MatchRepository matchRepository;
    private final SeasonRepository seasonRepository;
    private final MatchProjectionMaintenanceRepository maintenanceRepository;
    private final MatchService matchService;
    private final SeasonService seasonService;

    /**
     * Repairs only the projections owned by one match. This is the fast path when the
     * stored match payload is correct and only derived statistics need to be regenerated.
     */
    @Transactional
    @CacheEvict(value = {
            CacheNames.ALL_SEASONS,
            CacheNames.ALL_TEAMS,
            CacheNames.SEASON_TEAMS,
            CacheNames.SEASON_MATCHES,
            CacheNames.BATTING_LEADERBOARD,
            CacheNames.BOWLING_LEADERBOARD,
            CacheNames.FIELDING_LEADERBOARD
    }, allEntries = true)
    public RebuildResultDto rebuildMatch(String matchId) {
        maintenanceRepository.acquireProjectionWriteLock();
        Match match = findMatch(matchId);
        String seasonId = match.getSeason().getId();

        maintenanceRepository.deleteMatchProjections(matchId);
        matchService.replayStoredMatch(match);
        seasonService.syncMatchesPlayed(seasonId);

        log.info("Match projections rebuilt: matchId={}, seasonId={}", matchId, seasonId);
        return new RebuildResultDto(
                "MATCH", matchId, seasonId, 1,
                "Match projections rebuilt from stored match_data"
        );
    }

    /**
     * Deletes a scrap match and then rebuilds its entire season. Rebuilding the season is
     * deliberate: it removes stale season_players/player_teams/team_season memberships that
     * may have existed only because of the deleted match.
     */
    @Transactional
    @CacheEvict(value = {
            CacheNames.ALL_SEASONS,
            CacheNames.ALL_TEAMS,
            CacheNames.SEASON_TEAMS,
            CacheNames.SEASON_MATCHES,
            CacheNames.BATTING_LEADERBOARD,
            CacheNames.BOWLING_LEADERBOARD,
            CacheNames.FIELDING_LEADERBOARD
    }, allEntries = true)
    public RebuildResultDto deleteMatchAndRebuildSeason(String matchId) {
        maintenanceRepository.acquireProjectionWriteLock();
        Match match = findMatch(matchId);
        String seasonId = match.getSeason().getId();

        matchRepository.delete(match);
        matchRepository.flush();

        int replayed = rebuildSeasonInternal(seasonId);
        log.info("Match deleted and season rebuilt: matchId={}, seasonId={}, replayed={}",
                matchId, seasonId, replayed);

        return new RebuildResultDto(
                "DELETE_MATCH", matchId, seasonId, replayed,
                "Match deleted and season projections rebuilt from remaining matches"
        );
    }

    @Transactional
    @CacheEvict(value = {
            CacheNames.ALL_SEASONS,
            CacheNames.ALL_TEAMS,
            CacheNames.SEASON_TEAMS,
            CacheNames.SEASON_MATCHES,
            CacheNames.BATTING_LEADERBOARD,
            CacheNames.BOWLING_LEADERBOARD,
            CacheNames.FIELDING_LEADERBOARD
    }, allEntries = true)
    public RebuildResultDto rebuildSeason(String seasonId) {
        maintenanceRepository.acquireProjectionWriteLock();
        if (!seasonRepository.existsById(seasonId)) {
            throw new ResourceNotFoundException("Season not found: " + seasonId);
        }

        int replayed = rebuildSeasonInternal(seasonId);
        log.info("Season projections rebuilt: seasonId={}, replayed={}", seasonId, replayed);

        return new RebuildResultDto(
                "SEASON", null, seasonId, replayed,
                "Season projections rebuilt from stored matches"
        );
    }

    @Transactional
    @CacheEvict(value = {
            CacheNames.ALL_SEASONS,
            CacheNames.ALL_TEAMS,
            CacheNames.SEASON_TEAMS,
            CacheNames.SEASON_MATCHES,
            CacheNames.BATTING_LEADERBOARD,
            CacheNames.BOWLING_LEADERBOARD,
            CacheNames.FIELDING_LEADERBOARD
    }, allEntries = true)
    public RebuildResultDto rebuildAll() {
        maintenanceRepository.acquireProjectionWriteLock();
        List<String> matchIds = matchRepository.findAllIdsForReplay();
        maintenanceRepository.deleteAllProjections();

        for (String matchId : matchIds) {
            matchService.replayStoredMatch(findMatch(matchId));
        }
        seasonService.syncAllMatchesPlayed();

        log.info("All match projections rebuilt: replayed={}", matchIds.size());
        return new RebuildResultDto(
                "ALL", null, null, matchIds.size(),
                "All projections rebuilt from stored matches"
        );
    }

    private int rebuildSeasonInternal(String seasonId) {
        List<String> matchIds = matchRepository.findIdsBySeasonIdForReplay(seasonId);
        maintenanceRepository.deleteSeasonProjections(seasonId);

        for (String matchId : matchIds) {
            matchService.replayStoredMatch(findMatch(matchId));
        }
        seasonService.syncMatchesPlayed(seasonId);
        return matchIds.size();
    }

    private Match findMatch(String matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found: " + matchId));
    }
}
