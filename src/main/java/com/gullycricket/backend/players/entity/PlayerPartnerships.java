    package com.gullycricket.backend.players.entity;

    import com.gullycricket.backend.matches.entity.Match;
    import com.gullycricket.backend.matches.entity.MatchFormat;
    import com.gullycricket.backend.seasons.entity.Season;
    import com.gullycricket.backend.teams.entity.Team;
    import jakarta.persistence.*;
    import lombok.AllArgsConstructor;
    import lombok.Getter;
    import lombok.NoArgsConstructor;
    import lombok.Setter;

    @Entity
    @Table(
            name = "player_partnerships",
            uniqueConstraints = {
                    @UniqueConstraint(
                            columnNames = {"player1_id", "player2_id", "match_id", "innings_number", "partnership_number"}
                    )
            }
    )
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public class PlayerPartnerships {

        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        private String id;

        @ManyToOne(optional = false)
        @JoinColumn(name = "player1_id")
        private Player player1;

        @ManyToOne(optional = false)
        @JoinColumn(name = "player2_id")
        private Player player2;

        @ManyToOne(optional = false)
        @JoinColumn(name = "match_id")
        private Match match;

        @ManyToOne(optional = false)
        @JoinColumn(name = "season_id")
        private Season season;

        @ManyToOne(optional = false)
        @JoinColumn(name = "team_represented_id")
        private Team teamRepresented;

        @Enumerated(EnumType.STRING)
        private MatchFormat matchType;          // LIMITED_OVERS or TEST_MATCH

        @Column(nullable = false)
        private Integer inningsNumber = 1;       // 1 or 2 — which round this partnership happened in

        @Column(nullable = false)
        private Integer runsScored = 0;

        @Column(nullable = false)
        private Integer ballsFaced = 0;

        @Column(nullable = false)
        private Integer dotBalls = 0;

        @Column(nullable = false)
        private Integer foursHit = 0;

        @Column(nullable = false)
        private Integer sixesHit = 0;

        @Column(nullable = false)
        private boolean matchWon = false;

        @Column(nullable = false)
        private boolean battingFirst = false;

        @Column(nullable = false)
        private Integer partnershipNumber = 0;   // 1 = opening, 2 = for 1st wicket, etc.

        @Column(nullable = false)
        private boolean partnershipBroken = false;

        @ManyToOne
        @JoinColumn(name = "who_got_out_id")
        private Player whoGotOut;

        @Column(nullable = false)
        private Integer player1Runs = 0;
        @Column(nullable = false)
        private Integer player1BallsFaced = 0;
        @Column(nullable = false)
        private Integer player1DotBalls = 0;
        @Column(nullable = false)
        private Integer player1FoursHit = 0;
        @Column(nullable = false)
        private Integer player1SixesHit = 0;

        @Column(nullable = false)
        private Integer player2Runs = 0;
        @Column(nullable = false)
        private Integer player2BallsFaced = 0;
        @Column(nullable = false)
        private Integer player2DotBalls = 0;
        @Column(nullable = false)
        private Integer player2FoursHit = 0;
        @Column(nullable = false)
        private Integer player2SixesHit = 0;
    }