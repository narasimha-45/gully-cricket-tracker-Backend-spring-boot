package com.gullycricket.backend.matches.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.gullycricket.backend.seasons.entity.Season;
import com.gullycricket.backend.teams.entity.Team;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "season_id")
    private Season season;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchType matchType;

    @ManyToOne(optional = false)
    @JoinColumn(name = "team_a_id")
    private Team teamA;

    @ManyToOne(optional = false)
    @JoinColumn(name = "team_b_id")
    private Team teamB;

    @Column(nullable = false)
    private Integer teamAScore = 0;

    @Column(nullable = false)
    private Integer teamAWickets = 0;

    @Column(nullable = false)
    private Integer teamABallsFaced = 0;

    @Column(nullable = false)
    private Integer teamBScore = 0;

    @Column(nullable = false)
    private Integer teamBWickets = 0;

    @Column(nullable = false)
    private Integer teamBBallsFaced = 0;

    private Integer totalOvers;

    @ManyToOne
    @JoinColumn(name = "winner_team_id")
    private Team winnerTeam;

    @Column(nullable = false)
    private Boolean superOver = false;

    private Boolean isBattingFirstTeamWon = false;

    @ManyToOne
    @JoinColumn(name = "batting_first_team_id")
    private Team battingFirstTeam;

    @ManyToOne
    @JoinColumn(name = "batting_second_team_id")
    private Team battingSecondTeam;

    private Integer winByRuns;

    private Integer winByWickets;

    private Boolean isInningsWin = false;

    private Boolean isMatchDrawn = false;

    private Boolean isMatchTied = false;

    private String wonBy;

    @OneToMany(
            mappedBy = "match",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<MatchInningsSummary> inningsSummaries =
            new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode matchData;

    private LocalDateTime completedAt;
}