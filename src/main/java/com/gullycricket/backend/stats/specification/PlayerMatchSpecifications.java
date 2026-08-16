package com.gullycricket.backend.stats.specification;

import com.gullycricket.backend.matches.entity.MatchType;
import com.gullycricket.backend.players.entity.PlayerMatch;
import com.gullycricket.backend.stats.enums.MatchResult;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class PlayerMatchSpecifications {

    private PlayerMatchSpecifications() {
    }

    public static Specification<PlayerMatch> playerId(String playerId) {
        return (root, query, cb) -> playerId == null ? null : cb.equal(root.get("player").get("id"), playerId);
    }

    public static Specification<PlayerMatch> seasonId(String seasonId) {
        return (root, query, cb) -> seasonId == null ? null : cb.equal(root.get("season").get("id"), seasonId);
    }

    public static Specification<PlayerMatch> matchType(MatchType matchType) {
        return (root, query, cb) -> matchType == null ? null : cb.equal(root.get("matchType"), matchType);
    }

    public static Specification<PlayerMatch> teamId(String teamId) {
        return (root, query, cb) -> teamId == null ? null : cb.equal(root.get("teamRepresented").get("id"), teamId);
    }

    public static Specification<PlayerMatch> opponentTeamId(String opponentTeamId) {
        return (root, query, cb) -> opponentTeamId == null ? null : cb.equal(root.get("oppositionTeam").get("id"), opponentTeamId);
    }

    public static Specification<PlayerMatch> inningsNumber(Integer inningsNumber) {
        return (root, query, cb) -> inningsNumber == null ? null : cb.equal(root.get("inningsNumber"), inningsNumber);
    }

    public static Specification<PlayerMatch> battingPosition(Integer battingPosition) {
        return (root, query, cb) -> battingPosition == null ? null : cb.equal(root.get("battingPosition"), battingPosition);
    }

    public static Specification<PlayerMatch> batted() {
        return (root, query, cb) -> cb.isTrue(root.get("batted"));
    }

    public static Specification<PlayerMatch> bowled() {
        return (root, query, cb) -> cb.isTrue(root.get("bowled"));
    }

    public static Specification<PlayerMatch> matchResult(MatchResult result) {
        return (root, query, cb) -> {
            if (result == null) {
                return null;
            }
            var matchJoin = root.join("match", JoinType.INNER);
            var tied = cb.coalesce(matchJoin.<Boolean>get("isMatchTied"), Boolean.FALSE);
            var drawn = cb.coalesce(matchJoin.<Boolean>get("isMatchDrawn"), Boolean.FALSE);
            var winnerMissing = cb.isNull(matchJoin.get("winnerTeam"));
            return switch (result) {
                case WIN -> cb.and(cb.isTrue(root.<Boolean>get("matchWon")), cb.isFalse(tied), cb.isFalse(drawn));
                case LOSS -> cb.and(cb.isFalse(root.<Boolean>get("matchWon")), cb.isFalse(tied), cb.isFalse(drawn), cb.not(winnerMissing));
                case TIE -> cb.isTrue(tied);
                case NO_RESULT -> cb.or(cb.isTrue(drawn), cb.and(winnerMissing, cb.isFalse(tied)));
            };
        };
    }

    /**
     * Combines the common filter fields shared by all stats filter DTOs into a single Specification.
     */
    public static Specification<PlayerMatch> withCommonFilters(
            String seasonId,
            MatchType matchType,
            String teamId,
            String opponentTeamId,
            Integer inningsNumber,
            MatchResult result
    ) {
        List<Specification<PlayerMatch>> specs = new ArrayList<>();
        specs.add(seasonId(seasonId));
        specs.add(matchType(matchType));
        specs.add(teamId(teamId));
        specs.add(opponentTeamId(opponentTeamId));
        specs.add(inningsNumber(inningsNumber));
        specs.add(matchResult(result));

        Specification<PlayerMatch> combined = Specification.where(null);
        for (Specification<PlayerMatch> spec : specs) {
            combined = combined.and(spec);
        }
        return combined;
    }
}
