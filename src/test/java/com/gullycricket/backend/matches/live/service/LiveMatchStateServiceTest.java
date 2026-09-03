package com.gullycricket.backend.matches.live.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LiveMatchStateServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LiveMatchStateService service = new LiveMatchStateService(60_000);

    @Test
    void appliesIncrementalBallPatchAndBuildsSeasonSummary() {
        service.sync("match-1", "season-1", "token-1", 1, snapshot());

        ObjectNode patch = objectMapper.createObjectNode();
        ObjectNode topLevel = patch.putObject("topLevel");
        topLevel.put("status", "LIVE");
        topLevel.put("updatedAt", 2);
        patch.set("live", objectMapper.createObjectNode().put("inningsIndex", 0));

        ArrayNode inningsPatches = patch.putArray("innings");
        ObjectNode inningsPatch = inningsPatches.addObject();
        inningsPatch.put("inningsIndex", 0);
        ObjectNode summary = inningsPatch.putObject("summary");
        summary.put("battingTeam", "eagles");
        summary.put("bowlingTeam", "spiders");
        summary.put("inningsNumber", 1);
        summary.put("totalRuns", 4);
        summary.put("wickets", 0);
        summary.put("balls", 1);
        summary.set("battingStats", objectMapper.createObjectNode());
        summary.set("bowlingStats", objectMapper.createObjectNode());
        summary.set("dismissals", objectMapper.createObjectNode());
        summary.set("thisOver", objectMapper.createArrayNode());
        summary.set("extras", objectMapper.createObjectNode().put("wides", 0).put("noBalls", 0));

        ObjectNode delta = patch.putObject("ballDelta");
        delta.put("inningsIndex", 0);
        delta.put("truncateTo", 0);
        delta.putArray("append").addObject()
                .put("type", "RUN")
                .put("runs", 4)
                .put("ballInOver", 1);

        service.applyPatch("match-1", "season-1", "token-1", 2, patch);

        var live = service.get("match-1").orElseThrow();
        assertThat(live.match().path("innings").get(0).path("totalRuns").asInt()).isEqualTo(4);
        assertThat(live.match().path("innings").get(0).path("ballByBall").size()).isEqualTo(1);

        var summaries = service.listBySeason("season-1");
        assertThat(summaries).hasSize(1);
        assertThat(summaries.getFirst().teamA()).isEqualTo("eagles");
        assertThat(summaries.getFirst().runs()).isEqualTo(4);
        assertThat(summaries.getFirst().tossWinner()).isEqualTo("eagles");
        assertThat(summaries.getFirst().tossDecision()).isEqualTo("bat");
    }

    @Test
    void rejectsDifferentScorerToken() {
        service.sync("match-1", "season-1", "token-1", 1, snapshot());

        assertThatThrownBy(() -> service.sync(
                "match-1",
                "season-1",
                "other-token",
                2,
                snapshot()
        )).isInstanceOf(LiveMatchStateService.UnauthorizedLiveScorerException.class);
    }

    private ObjectNode snapshot() {
        ObjectNode match = objectMapper.createObjectNode();
        match.put("id", "match-1");
        match.put("seasonId", "season-1");
        match.put("status", "LIVE");
        match.put("matchType", "OVERS");
        match.put("totalOvers", 20);
        match.put("updatedAt", 1);
        match.putObject("toss")
                .put("winner", "eagles")
                .put("decision", "bat");
        ObjectNode teams = match.putObject("teams");
        teams.putObject("teamA").put("name", "eagles");
        teams.putObject("teamB").put("name", "spiders");
        match.putObject("live")
                .put("inningsIndex", 0)
                .set("history", objectMapper.createArrayNode().addObject().put("secret", true));
        ObjectNode innings = match.putArray("innings").addObject();
        innings.put("battingTeam", "eagles");
        innings.put("bowlingTeam", "spiders");
        innings.put("inningsNumber", 1);
        innings.put("totalRuns", 0);
        innings.put("wickets", 0);
        innings.put("balls", 0);
        innings.set("battingStats", objectMapper.createObjectNode());
        innings.set("bowlingStats", objectMapper.createObjectNode());
        innings.set("dismissals", objectMapper.createObjectNode());
        innings.set("thisOver", objectMapper.createArrayNode());
        innings.set("ballByBall", objectMapper.createArrayNode());
        innings.set("extras", objectMapper.createObjectNode().put("wides", 0).put("noBalls", 0));
        match.putObject("liveScoring").put("scorerToken", "must-not-survive");
        return match;
    }
}
