package com.gullycricket.backend.matches.service;

import com.gullycricket.backend.common.exception.BadRequestException;
import com.gullycricket.backend.common.util.NameNormalizer;
import com.gullycricket.backend.matches.dto.*;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
public class MatchValidator {

    public void validate(MatchDataDto dto) {
        require(dto != null, "Match payload is required");
        require(hasText(dto.seasonId()), "seasonId is required");
        require(dto.matchType() != null, "matchType is required");
        require(dto.totalOvers() >= 0, "totalOvers cannot be negative");
        require(dto.teams() != null, "teams are required");

        TeamDto teamA = dto.teams().get("teamA");
        TeamDto teamB = dto.teams().get("teamB");
        validateTeam(teamA, "teamA");
        validateTeam(teamB, "teamB");

        String teamAName = canonical(teamA.name());
        String teamBName = canonical(teamB.name());
        require(!Objects.equals(teamAName, teamBName), "teamA and teamB must be different teams");

        Set<String> teamAPlayers = canonicalPlayers(teamA.players());
        Set<String> teamBPlayers = canonicalPlayers(teamB.players());

        // Gully-cricket domain rule: the same physical player may act as a "joker"
        // and represent both teams in the same match. We intentionally validate
        // uniqueness only within each individual team squad, not across squads.

        List<InningsDto> innings = dto.innings();
        require(innings != null && !innings.isEmpty(), "At least one innings is required");

        long regularInnings = innings.stream().filter(i -> i != null && !i.isSuperOver()).count();
        require(regularInnings > 0, "At least one regular innings is required");

        for (int i = 0; i < innings.size(); i++) {
            validateInnings(innings.get(i), i + 1, teamAName, teamBName, teamAPlayers, teamBPlayers);
        }

        if (dto.result() != null) {
            String winner = canonical(dto.result().winner());
            if (hasText(winner)) {
                require(winner.equals(teamAName) || winner.equals(teamBName), "result.winner must be teamA or teamB");
            }
            String motm = canonical(dto.result().manOfTheMatch());
            if (hasText(motm)) {
                require(teamAPlayers.contains(motm) || teamBPlayers.contains(motm), "result.manOfTheMatch must be a player in this match");
            }
        }

        if (dto.toss() != null && hasText(dto.toss().winner())) {
            String tossWinner = canonical(dto.toss().winner());
            require(tossWinner.equals(teamAName) || tossWinner.equals(teamBName), "toss.winner must be teamA or teamB");
        }
    }

    private void validateTeam(TeamDto team, String label) {
        require(team != null, label + " is required");
        require(hasText(team.name()), label + ".name is required");
        require(team.players() != null && !team.players().isEmpty(), label + ".players cannot be empty");

        Set<String> unique = new HashSet<>();
        for (String player : team.players()) {
            require(hasText(player), label + " contains a blank player name");
            String canonical = canonical(player);
            require(unique.add(canonical), label + " contains duplicate player: " + canonical);
        }
    }

    private void validateInnings(InningsDto inning, int sequence, String teamAName, String teamBName, Set<String> teamAPlayers, Set<String> teamBPlayers) {
        require(inning != null, "innings[" + (sequence - 1) + "] is required");
        String battingTeam = canonical(inning.battingTeam());
        String bowlingTeam = canonical(inning.bowlingTeam());

        require(hasText(battingTeam), "innings " + sequence + " battingTeam is required");
        require(hasText(bowlingTeam), "innings " + sequence + " bowlingTeam is required");
        require(!battingTeam.equals(bowlingTeam), "innings " + sequence + " batting and bowling teams must differ");
        require(isKnownTeam(battingTeam, teamAName, teamBName), "innings " + sequence + " has unknown battingTeam");
        require(isKnownTeam(bowlingTeam, teamAName, teamBName), "innings " + sequence + " has unknown bowlingTeam");
        require(inning.totalRuns() >= 0, "innings " + sequence + " totalRuns cannot be negative");
        require(inning.wickets() >= 0, "innings " + sequence + " wickets cannot be negative");
        require(inning.balls() >= 0, "innings " + sequence + " balls cannot be negative");

        Set<String> battingPlayers = battingTeam.equals(teamAName) ? teamAPlayers : teamBPlayers;
        Set<String> bowlingPlayers = bowlingTeam.equals(teamAName) ? teamAPlayers : teamBPlayers;

        if (inning.battingStats() != null) {
            inning.battingStats().forEach((player, stat) -> {
                String p = canonical(player);
                require(battingPlayers.contains(p), "Batting stat player is not in batting team: " + p);
                if (stat != null) {
                    require(stat.runs() >= 0 && stat.balls() >= 0 && stat.fours() >= 0 && stat.sixes() >= 0, "Batting stats cannot contain negative values for " + p);
                }
            });
        }

        if (inning.bowlingStats() != null) {
            inning.bowlingStats().forEach((player, stat) -> {
                String p = canonical(player);
                require(bowlingPlayers.contains(p), "Bowling stat player is not in bowling team: " + p);
                if (stat != null) {
                    require(stat.balls() >= 0 && stat.runs() >= 0 && stat.wickets() >= 0 && stat.maidens() >= 0, "Bowling stats cannot contain negative values for " + p);
                }
            });
        }

        if (inning.ballByBall() != null) {
            for (BallDto ball : inning.ballByBall()) {
                if (ball == null) {
                    continue;
                }

                require(ball.type() != null, "Ball type is required");

                if (ball.type() == BallType.RETIRE) {

                    require(battingPlayers.contains(canonical(ball.striker())), "Unknown retired player: " + ball.striker());

                    if (hasText(ball.nonStriker())) {
                        require(battingPlayers.contains(canonical(ball.nonStriker())), "Unknown non-striker: " + ball.nonStriker());
                    }

                    continue;
                }
                require(ball.runs() >= 0, "Ball runs cannot be negative");
                require(battingPlayers.contains(canonical(ball.striker())), "Unknown striker: " + ball.striker());
                if (hasText(ball.nonStriker())) {
                    require(battingPlayers.contains(canonical(ball.nonStriker())), "Unknown non-striker: " + ball.nonStriker());
                }
                require(bowlingPlayers.contains(canonical(ball.bowler())), "Unknown bowler: " + ball.bowler());

                if (ball.isWicket()) {
                    require(ball.wicket() != null, "Wicket details are required when isWicket=true");
                    require(battingPlayers.contains(canonical(ball.wicket().outBatsman())), "Wicket outBatsman is not in batting team: " + ball.wicket().outBatsman());
                    if (hasText(ball.wicket().helper())) {
                        require(bowlingPlayers.contains(canonical(ball.wicket().helper())), "Wicket helper is not in fielding team: " + ball.wicket().helper());
                    }
                }
            }
        }
    }

    private boolean isKnownTeam(String team, String teamA, String teamB) {
        return team.equals(teamA) || team.equals(teamB);
    }

    private Set<String> canonicalPlayers(List<String> players) {
        Set<String> result = new HashSet<>();
        if (players != null) {
            players.stream().map(this::canonical).filter(this::hasText).forEach(result::add);
        }
        return result;
    }

    private String canonical(String value) {
        return NameNormalizer.normalize(value);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new BadRequestException(message);
        }
    }
}
