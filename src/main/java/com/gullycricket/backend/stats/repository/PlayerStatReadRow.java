package com.gullycricket.backend.stats.repository;

import java.time.LocalDateTime;

/** Lightweight player-match read row; deliberately contains no match JSON. */
public record PlayerStatReadRow(
        String playerId,
        String playerName,
        String matchId,
        String seasonId,
        String seasonName,
        String teamId,
        String teamName,
        String opponentTeamId,
        String opponentTeamName,
        Integer inningsNumber,
        boolean matchWon,
        boolean playerOfTheMatch,
        boolean batted,
        Integer battingPosition,
        int runsScored,
        int ballsFaced,
        int foursHit,
        int sixesHit,
        boolean out,
        int dotBallsPlayed,
        boolean bowled,
        int wicketsTaken,
        int ballsBowled,
        int runsConceded,
        int maidensBowled,
        int dotBallsBowled,
        int catchesTaken,
        int runOuts,
        int stumpings,
        boolean matchTied,
        boolean matchDrawn,
        String winnerTeamId,
        LocalDateTime completedAt
) {
}
