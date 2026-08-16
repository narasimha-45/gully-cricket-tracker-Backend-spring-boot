package com.gullycricket.backend.matches.entity;

import com.gullycricket.backend.teams.entity.Team;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "match_innings_summary",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_match_innings_sequence", columnNames = {"match_id", "sequence_number"}),
                @UniqueConstraint(name = "uk_match_team_innings", columnNames = {"match_id", "batting_team_id", "team_innings_number"})
        },
        indexes = {
                @Index(name = "idx_innings_match_id", columnList = "match_id"),
                @Index(name = "idx_innings_batting_team_id", columnList = "batting_team_id"),
                @Index(name = "idx_innings_bowling_team_id", columnList = "bowling_team_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MatchInningsSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "batting_team_id", nullable = false)
    private Team battingTeam;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "bowling_team_id", nullable = false)
    private Team bowlingTeam;

    /** Absolute innings order within the match: 1,2,3,4. */
    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    /** Number of times this team has batted in the match: 1 or 2 for Test cricket. */
    @Column(nullable = false)
    private Integer teamInningsNumber;

    @Column(nullable = false)
    private Integer runs = 0;

    @Column(nullable = false)
    private Integer wickets = 0;

    @Column(nullable = false)
    private Integer balls = 0;

    @Column(nullable = false)
    private boolean superOver = false;

    @Column(nullable = false)
    private boolean completed = false;
}
