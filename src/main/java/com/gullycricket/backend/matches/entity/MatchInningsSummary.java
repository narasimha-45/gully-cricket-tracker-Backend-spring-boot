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
                @UniqueConstraint(
                        columnNames = {
                                "match_id",
                                "batting_team_id",
                                "team_innings_number"
                        }
                )
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
    @JoinColumn(name = "match_id")
    private Match match;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "batting_team_id")
    private Team battingTeam;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "bowling_team_id")
    private Team bowlingTeam;

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