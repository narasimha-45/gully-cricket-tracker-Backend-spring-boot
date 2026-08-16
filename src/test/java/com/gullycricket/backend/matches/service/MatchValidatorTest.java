package com.gullycricket.backend.matches.service;

import com.gullycricket.backend.common.exception.BadRequestException;
import com.gullycricket.backend.matches.dto.*;
import com.gullycricket.backend.matches.entity.MatchType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchValidatorTest {

    private final MatchValidator validator = new MatchValidator();

    @Test
    void acceptsMixedCaseTeamReferencesBecauseTheyCanonicalizeToSameTeam() {
        MatchDataDto dto = baseDto(new ResultDto("EAGLES", "RUNS", 10, "Alice"));
        assertThatCode(() -> validator.validate(dto)).doesNotThrowAnyException();
    }

    @Test
    void rejectsWinnerOutsideTheMatch() {
        MatchDataDto dto = baseDto(new ResultDto("Other Team", "RUNS", 10, "Alice"));
        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("result.winner");
    }

    @Test
    void allowsJokerPlayerToRepresentBothTeamsInSameMatch() {
        MatchDataDto dto = new MatchDataDto(
                "season-1",
                Map.of(
                        "teamA", new TeamDto("Eagles", List.of("Alice", "Shared")),
                        "teamB", new TeamDto("Spiders", List.of("Bob", "Shared"))
                ),
                new TossDto("Eagles", "BAT"),
                rules(),
                10,
                MatchType.OVERS,
                List.of(innings("Eagles", "Spiders"), innings("Spiders", "Eagles")),
                new ResultDto(null, "TIE", 0, null)
        );

        assertThatCode(() -> validator.validate(dto)).doesNotThrowAnyException();
    }

    private MatchDataDto baseDto(ResultDto result) {
        return new MatchDataDto(
                "season-1",
                Map.of(
                        "teamA", new TeamDto("Eagles", List.of("Alice", "Amy")),
                        "teamB", new TeamDto("Spiders", List.of("Bob", "Ben"))
                ),
                new TossDto("eagles", "BAT"),
                rules(),
                10,
                MatchType.OVERS,
                List.of(innings("EAGLES", "spiders"), innings("Spiders", "Eagles")),
                result
        );
    }

    private InningsDto innings(String batting, String bowling) {
        return new InningsDto(
                batting, bowling, 80, 2, 60,
                Map.of(), Map.of(), new ExtrasDto(0, 0), Map.of(), List.of(), false, true
        );
    }

    private RulesDto rules() {
        return new RulesDto(new RuleDetailDto(true, true), new RuleDetailDto(true, true));
    }
}
