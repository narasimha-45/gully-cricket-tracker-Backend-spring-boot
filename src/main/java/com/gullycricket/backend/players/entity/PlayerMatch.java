package com.gullycricket.backend.players.entity;

import com.gullycricket.backend.matches.entity.Match;
import com.gullycricket.backend.matches.entity.MatchType;
import com.gullycricket.backend.seasons.entity.Season;
import com.gullycricket.backend.teams.entity.Team;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "player_matches",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"player_id", "match_id", "team_represented_id", "innings_number"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlayerMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "player_id")
    private Player player;

    @ManyToOne(optional = false)
    @JoinColumn(name = "match_id")
    private Match match;

    @ManyToOne(optional = false)
    @JoinColumn(name = "season_id")
    private Season season;

    @ManyToOne(optional = false)
    @JoinColumn(name = "team_represented_id")
    private Team teamRepresented;

    @ManyToOne(optional = false)
    @JoinColumn(name = "opposition_team_id")
    private Team oppositionTeam;

    @Enumerated(EnumType.STRING)
    private MatchType matchType;        // LIMITED_OVERS or TEST_MATCH

    @Column(nullable = false)
    private Integer inningsNumber = 1;     // 1 = 1st innings round, 2 = 2nd innings round (Test only)

    // =========================
    // Match Context
    // =========================

    @Column(nullable = false)
    private boolean matchWon = false;

    @Column(nullable = false)
    private boolean playerOfTheMatch = false;

    // =========================
    // Batting
    // =========================

    @Column(nullable = false)
    private boolean batted = false;

    private Integer battingPosition;

    @Column(nullable = false)
    private boolean battingFirst = false;

    @Column(nullable = false)
    private Integer runsScored = 0;

    @Column(nullable = false)
    private Integer ballsFaced = 0;

    @Column(nullable = false)
    private Integer foursHit = 0;

    @Column(nullable = false)
    private Integer sixesHit = 0;

    @Column(nullable = false)
    private boolean out = false;

    @Column(nullable = false)
    private Integer dotBallsPlayed = 0;

    @Enumerated(EnumType.STRING)
    private DismissalType dismissalType;

    // =========================
    // Bowling
    // =========================

    @Column(nullable = false)
    private boolean bowled = false;

    @Column(nullable = false)
    private boolean bowlingFirst = false;

    @Column(nullable = false)
    private Integer wicketsTaken = 0;

    @Column(nullable = false)
    private Integer ballsBowled = 0;

    @Column(nullable = false)
    private Integer runsConceded = 0;

    @Column(nullable = false)
    private Integer maidensBowled = 0;

    @Column(nullable = false)
    private Integer widesBowled = 0;

    @Column(nullable = false)
    private Integer noBallsBowled = 0;

    @Column(nullable = false)
    private Integer bowledDismissals = 0;

    @Column(nullable = false)
    private Integer caughtDismissals = 0;

    @Column(nullable = false)
    private Integer lbwDismissals = 0;

    @Column(nullable = false)
    private Integer stumpedDismissals = 0;

    @Column(nullable = false)
    private Integer hitWicketDismissals = 0;

    @Column(nullable = false)
    private Integer specialWicketDismissals = 0;

    @Column(nullable = false)
    private Integer dotBallsBowled = 0;

    // =========================
    // Fielding
    // =========================

    @Column(nullable = false)
    private Integer catchesTaken = 0;

    @Column(nullable = false)
    private Integer runOuts = 0;

    @Column(nullable = false)
    private Integer stumpings = 0;
}