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
@Table(
        name = "matches",
        indexes = {
                @Index(name = "idx_matches_season_id", columnList = "season_id"),
                @Index(name = "idx_matches_team_a_id", columnList = "team_a_id"),
                @Index(name = "idx_matches_team_b_id", columnList = "team_b_id"),
                @Index(name = "idx_matches_status", columnList = "status"),
                @Index(name = "idx_matches_season_status_completed", columnList = "season_id,status,completed_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id")
    private Season season;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MatchStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false)
    private MatchType matchType;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "team_a_id")
    private Team teamA;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "team_b_id")
    private Team teamB;

    @Column(name = "team_a_score", nullable = false)
    private Integer teamAScore = 0;

    @Column(name = "team_a_wickets", nullable = false)
    private Integer teamAWickets = 0;

    @Column(name = "team_a_balls_faced", nullable = false)
    private Integer teamABallsFaced = 0;

    @Column(name = "team_b_score", nullable = false)
    private Integer teamBScore = 0;

    @Column(name = "team_b_wickets", nullable = false)
    private Integer teamBWickets = 0;

    @Column(name = "team_b_balls_faced", nullable = false)
    private Integer teamBBallsFaced = 0;

    @Column(name = "total_overs")
    private Integer totalOvers;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_team_id")
    private Team winnerTeam;

    @Column(name = "super_over", nullable = false)
    private Boolean superOver = false;

    @Column(name = "is_batting_first_team_won")
    private Boolean isBattingFirstTeamWon = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batting_first_team_id")
    private Team battingFirstTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batting_second_team_id")
    private Team battingSecondTeam;

    @Column(name = "win_by_runs")
    private Integer winByRuns;

    @Column(name = "win_by_wickets")
    private Integer winByWickets;

    @Column(name = "is_innings_win")
    private Boolean isInningsWin = false;

    @Column(name = "is_match_drawn")
    private Boolean isMatchDrawn = false;

    @Column(name = "is_match_tied")
    private Boolean isMatchTied = false;

    @Column(name = "won_by")
    private String wonBy;

    /** Test-match config: how many innings per side (1 or 2). Null for OVERS matches. */
    @Column(name = "test_innings_per_team")
    private Integer testInningsPerTeam;

    /** Whether the follow-on was enforced at any point in this Test match. */
    @Column(name = "follow_on_enforced", nullable = false)
    private Boolean followOnEnforced = false;

    @OneToMany(
            mappedBy = "match",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<MatchInningsSummary> inningsSummaries =
            new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "match_data", columnDefinition = "jsonb")
    private JsonNode matchData;

    @Column(name = "idempotency_key", unique = true, length = 128)
    private String idempotencyKey;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
