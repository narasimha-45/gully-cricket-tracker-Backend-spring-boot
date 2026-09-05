package com.gullycricket.backend.migration.service;

import com.gullycricket.backend.matches.dto.BattingStatDto;
import com.gullycricket.backend.matches.dto.BowlingStatDto;
import com.gullycricket.backend.matches.dto.DismissalDto;
import com.gullycricket.backend.matches.dto.ExtrasDto;
import com.gullycricket.backend.matches.dto.InningsDto;
import com.gullycricket.backend.matches.dto.MatchDataDto;
import com.gullycricket.backend.matches.dto.MatchResponseDto;
import com.gullycricket.backend.matches.dto.ResultDto;
import com.gullycricket.backend.matches.dto.RuleDetailDto;
import com.gullycricket.backend.matches.dto.RulesDto;
import com.gullycricket.backend.matches.dto.TeamDto;
import com.gullycricket.backend.matches.dto.TossDto;
import com.gullycricket.backend.matches.entity.Match;
import com.gullycricket.backend.matches.entity.MatchType;
import com.gullycricket.backend.matches.repository.MatchRepository;
import com.gullycricket.backend.matches.service.MatchService;
import com.gullycricket.backend.migration.dto.MigrationSummaryDto;
import com.gullycricket.backend.migration.dto.MongoBattingStatsDTO;
import com.gullycricket.backend.migration.dto.MongoBowlingStatsDTO;
import com.gullycricket.backend.migration.dto.MongoDismissalDTO;
import com.gullycricket.backend.migration.dto.MongoExtrasDTO;
import com.gullycricket.backend.migration.dto.MongoInningsDTO;
import com.gullycricket.backend.migration.dto.MongoResultDTO;
import com.gullycricket.backend.migration.dto.MongoRuleDTO;
import com.gullycricket.backend.migration.dto.MongoRulesDTO;
import com.gullycricket.backend.migration.dto.MongoTeamDTO;
import com.gullycricket.backend.migration.dto.MongoTossDTO;
import com.gullycricket.backend.migration.documents.MongoMatch;
import com.gullycricket.backend.migration.documents.MongoSeason;
import com.gullycricket.backend.migration.repository.MongoMatchRepository;
import com.gullycricket.backend.migration.repository.MongoSeasonRepository;
import com.gullycricket.backend.players.entity.DismissalType;
import com.gullycricket.backend.seasons.entity.Season;
import com.gullycricket.backend.seasons.service.SeasonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One-off migration of legacy Mongo match/season data into the Postgres schema.
 * <p>
 * Rather than re-implementing the score/result/player-stats derivation logic that
 * already lives in {@link MatchService} and {@code ProcessPlayerStatsService}, this
 * service reshapes each {@link MongoMatch} into a {@link MatchDataDto} and routes it
 * through {@link MatchService#saveMatch(MatchDataDto, String)} — the same path the live "create
 * match" endpoint uses. That keeps team/player resolution, score derivation, win
 * conditions, and player-stat aggregation in one place instead of two.
 * <p>
 * Known data gaps in the legacy Mongo shape (see {@link #toInningsDto}):
 * <ul>
 *     <li>No ball-by-ball data was ever captured, so partnership and rivalry stats
 *     cannot be reconstructed for migrated matches — those tables simply won't get
 *     rows for old matches.</li>
 *     <li>No per-bowler wide/no-ball breakdown was captured, so those two bowling
 *     fields come across as zero for migrated matches.</li>
 *     <li>No super-over flag was captured, so every migrated innings is treated as
 *     a regular (non-super-over) innings.</li>
 *     <li>Batting order wasn't captured — battingPosition is inferred from the
 *     iteration order of the battingStats map, which may not be reliable depending
 *     on how Mongo/BSON preserved key order.</li>
 *     <li>No declare/follow-on/completion-reason data was captured, and no match-level
 *     testConfig (innings-per-team, follow-on-enforced) either — every migrated Test
 *     innings comes across as isFollowOn=false with a null completionReason, and
 *     testConfig is left null.</li>
 * </ul>
 * Each match is migrated inside {@link MatchService#saveMatch}'s own transaction, so
 * one bad match rolls back on its own and doesn't abort the rest of the run. After
 * that transaction commits, {@code completedAt} is overwritten with the real date
 * from the Mongo document — {@code saveMatch} always stamps it with "now", which is
 * correct for the live create-match endpoint but wrong for migrating historical data.
 */
@Slf4j
@Profile("migration")
@Service
@RequiredArgsConstructor
public class MongoToPostgresMigrationService {

    private final MongoSeasonRepository mongoSeasonRepository;
    private final MongoMatchRepository mongoMatchRepository;
    private final SeasonService seasonService;
    private final MatchService matchService;
    private final MatchRepository matchRepository;

    public MigrationSummaryDto migrateData() {
        Map<String, String> seasonIdMap = migrateSeasons();
        log.info("Migrated {} seasons", seasonIdMap.size());

        List<MongoMatch> matches = mongoMatchRepository.findAll();
        int migrated = 0;
        List<String> failures = new ArrayList<>();

        for (MongoMatch match : matches) {
            try {
                migrateMatch(match, seasonIdMap);
                migrated++;
            } catch (Exception e) {
                log.error("Failed to migrate match {}: {}", match.getId(), e.getMessage(), e);
                failures.add(match.getId() + ": " + e.getMessage());
            }
        }

        log.info("Migration complete. {}/{} matches migrated, {} failures.",
                migrated, matches.size(), failures.size());

        return new MigrationSummaryDto(seasonIdMap.size(), matches.size(), migrated, failures);
    }

    private Map<String, String> migrateSeasons() {
        List<MongoSeason> seasons = mongoSeasonRepository.findAll();
        Map<String, String> seasonIdMap = new HashMap<>();

        for (MongoSeason season : seasons) {
            Season newSeason = seasonService.findSeasonByName(season.getSeasonName());
            if (newSeason == null) {
                newSeason = seasonService.createSeason(season.getSeasonName());
            }

            if (season.getCreatedAt() != null) {
                newSeason.setCreatedAt(season.getCreatedAt());
                newSeason = seasonService.updateSeason(newSeason);
            }

            seasonIdMap.put(season.getId(), newSeason.getId());
        }

        return seasonIdMap;
    }

    private void migrateMatch(MongoMatch match, Map<String, String> seasonIdMap) {
        String newSeasonId = seasonIdMap.get(match.getSeasonId());
        if (newSeasonId == null) {
            throw new IllegalStateException(
                    "No migrated season found for Mongo seasonId: " + match.getSeasonId());
        }

        MatchDataDto matchData = toMatchDataDto(match, newSeasonId);
        MatchResponseDto saved = matchService.saveMatch(matchData, "mongo:" + match.getId());

        // saveMatch() always stamps completedAt with "now" (correct for the live
        // create-match endpoint, wrong for a migration of historical matches) —
        // overwrite it with the real date from the Mongo document, if we have one.
        if (match.getCompletedAt() != null) {
            Match persisted = matchRepository.findById(saved.id())
                    .orElseThrow(() -> new IllegalStateException(
                            "Saved match not found immediately after save: " + saved.id()));
            persisted.setCompletedAt(match.getCompletedAt());
            matchRepository.save(persisted);
        }
    }

    private MatchDataDto toMatchDataDto(MongoMatch match, String newSeasonId) {
        Map<String, TeamDto> teams = new LinkedHashMap<>();
        if (match.getTeams() != null) {
            teams.put("teamA", toTeamDto(match.getTeams().teamA()));
            teams.put("teamB", toTeamDto(match.getTeams().teamB()));
        }

        return new MatchDataDto(
                newSeasonId,
                teams,
                toTossDto(match.getToss()),
                toRulesDto(match.getRules()),
                match.getTotalOvers() != null ? match.getTotalOvers() : 0,
                toMatchType(match.getMatchType()),
                null, // legacy Mongo shape never captured testConfig (inningsPerTeam/followOnEnforced)
                toInningsList(match.getInnings()),
                toResultDto(match.getResult())
        );
    }

    private TeamDto toTeamDto(MongoTeamDTO team) {
        if (team == null) {
            return new TeamDto(null, List.of());
        }
        return new TeamDto(
                normalizeTeamName(team.name()),
                team.players() != null ? team.players() : List.of()
        );
    }

    private TossDto toTossDto(MongoTossDTO toss) {
        if (toss == null) {
            return new TossDto(null, null);
        }
        return new TossDto(normalizeTeamName(toss.winner()), toss.decision());
    }

    private RulesDto toRulesDto(MongoRulesDTO rules) {
        if (rules == null) {
            return new RulesDto(new RuleDetailDto(false, false), new RuleDetailDto(false, false));
        }
        return new RulesDto(toRuleDetailDto(rules.wide()), toRuleDetailDto(rules.noBall()));
    }

    private RuleDetailDto toRuleDetailDto(MongoRuleDTO rule) {
        if (rule == null) {
            return new RuleDetailDto(false, false);
        }
        return new RuleDetailDto(
                Boolean.TRUE.equals(rule.extraRun()),
                Boolean.TRUE.equals(rule.extraBall())
        );
    }

    // The Postgres schema only distinguishes OVERS vs TEST. Anything not
    // explicitly "TEST" (case-insensitive) is treated as a limited-overs match.
    private MatchType toMatchType(String mongoMatchType) {
        if (mongoMatchType != null && mongoMatchType.equalsIgnoreCase("TEST")) {
            return MatchType.TEST;
        }
        return MatchType.OVERS;
    }

    private List<InningsDto> toInningsList(List<MongoInningsDTO> mongoInnings) {
        if (mongoInnings == null) {
            return List.of();
        }
        List<InningsDto> result = new ArrayList<>();
        Map<String, Integer> perTeamCounter = new HashMap<>();
        for (MongoInningsDTO inning : mongoInnings) {
            String battingTeam = normalizeTeamName(inning.battingTeam());
            int inningsNumber = perTeamCounter.merge(battingTeam, 1, Integer::sum);
            result.add(toInningsDto(inning, inningsNumber));
        }
        return result;
    }

    private InningsDto toInningsDto(MongoInningsDTO inning, int inningsNumber) {
        return new InningsDto(
                normalizeTeamName(inning.battingTeam()),
                normalizeTeamName(inning.bowlingTeam()),
                inningsNumber,
                inning.totalRuns() != null ? inning.totalRuns() : 0,
                inning.wickets() != null ? inning.wickets() : 0,
                inning.balls() != null ? inning.balls() : 0,
                toBattingStatsMap(inning.battingStats()),
                toBowlingStatsMap(inning.bowlingStats()),
                toExtrasDto(inning.extras()),
                toDismissalsMap(inning.dismissals()),
                List.of(),   // no ball-by-ball data in the legacy Mongo shape
                false,       // no super-over flag in the legacy Mongo shape
                false,       // no follow-on flag in the legacy Mongo shape
                Boolean.TRUE.equals(inning.completed()),
                null         // no completion reason (declared/all out/etc.) in the legacy Mongo shape
        );
    }

    private Map<String, BattingStatDto> toBattingStatsMap(Map<String, MongoBattingStatsDTO> mongoStats) {
        if (mongoStats == null) {
            return Map.of();
        }
        Map<String, BattingStatDto> result = new LinkedHashMap<>();
        int battingPosition = 1;
        for (Map.Entry<String, MongoBattingStatsDTO> entry : mongoStats.entrySet()) {
            MongoBattingStatsDTO stat = entry.getValue();
            result.put(entry.getKey(), new BattingStatDto(
                    battingPosition++,
                    stat.runs() != null ? stat.runs() : 0,
                    stat.balls() != null ? stat.balls() : 0,
                    stat.fours() != null ? stat.fours() : 0,
                    stat.sixes() != null ? stat.sixes() : 0,
                    toDismissalDto(stat.dismissal())
            ));
        }
        return result;
    }

    private Map<String, BowlingStatDto> toBowlingStatsMap(Map<String, MongoBowlingStatsDTO> mongoStats) {
        if (mongoStats == null) {
            return Map.of();
        }
        Map<String, BowlingStatDto> result = new LinkedHashMap<>();
        for (Map.Entry<String, MongoBowlingStatsDTO> entry : mongoStats.entrySet()) {
            MongoBowlingStatsDTO stat = entry.getValue();
            result.put(entry.getKey(), new BowlingStatDto(
                    stat.balls() != null ? stat.balls() : 0,
                    stat.runs() != null ? stat.runs() : 0,
                    stat.wickets() != null ? stat.wickets() : 0,
                    stat.maidens() != null ? stat.maidens() : 0,
                    0, // per-bowler no-ball count wasn't tracked in the legacy shape
                    0  // per-bowler wide count wasn't tracked in the legacy shape
            ));
        }
        return result;
    }

    private ExtrasDto toExtrasDto(MongoExtrasDTO extras) {
        if (extras == null) {
            return new ExtrasDto(0, 0, 0);
        }
        return new ExtrasDto(
                extras.wides() != null ? extras.wides() : 0,
                extras.noBalls() != null ? extras.noBalls() : 0,
                0 // legacy Mongo payload did not track byes
        );
    }

    private Map<String, DismissalDto> toDismissalsMap(Map<String, MongoDismissalDTO> mongoDismissals) {
        if (mongoDismissals == null) {
            return Map.of();
        }
        Map<String, DismissalDto> result = new LinkedHashMap<>();
        for (Map.Entry<String, MongoDismissalDTO> entry : mongoDismissals.entrySet()) {
            result.put(entry.getKey(), toDismissalDto(entry.getValue()));
        }
        return result;
    }

    private DismissalDto toDismissalDto(MongoDismissalDTO dismissal) {
        if (dismissal == null) {
            return null;
        }
        return new DismissalDto(
                parseDismissalType(dismissal.type()),
                dismissal.bowler(),
                dismissal.fielder()
        );
    }

    private DismissalType parseDismissalType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return DismissalType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unrecognized dismissal type '{}' — defaulting to SPECIAL_CASE", type);
            return DismissalType.SPECIAL;
        }
    }

    private ResultDto toResultDto(MongoResultDTO result) {
        if (result == null) {
            return new ResultDto(null, null, 0, null);
        }
        return new ResultDto(
                normalizeTeamName(result.winner()),
                result.type(),
                result.margin() != null ? result.margin() : 0,
                result.manOfTheMatch()
        );
    }

    private String normalizeTeamName(String teamName) {

        teamName =  teamName == null ? null : teamName.trim().toLowerCase();
        if(teamName != null && (teamName.equals("lokesh's team") || teamName.equals(("lokesh team")))){
            return "eagles";
        }
        if(teamName != null && (teamName.equals("narasimha's team") || teamName.equals(("narasimha team")))){
            return "spider";
        }
        return teamName;
    }
}
