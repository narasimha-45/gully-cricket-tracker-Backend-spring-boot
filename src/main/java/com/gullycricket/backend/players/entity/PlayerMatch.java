package com.gullycricket.backend.players.entity;

import com.gullycricket.backend.matches.entity.Match;
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
                @UniqueConstraint(columnNames = {"player_id", "match_id", "team_represented_id"})
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
    private Integer bowledDismissals = 0;

    @Column(nullable = false)
    private Integer caughtDismissals = 0;

    @Column(nullable = false)
    private Integer lbwDismissals = 0;

    @Column(nullable = false)
    private Integer stumpedDismissals = 0;

    @Column(nullable = false)
    private Integer runOutDismissals = 0;

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