package com.gullycricket.backend.players.entity;

import com.gullycricket.backend.matches.entity.Match;
import com.gullycricket.backend.seasons.entity.Season;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "player_rivalries",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"batsman_id", "bowler_id", "match_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlayerRivalry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "batsman_id")
    private Player batsman;

    @ManyToOne(optional = false)
    @JoinColumn(name = "bowler_id")
    private Player bowler;

    @ManyToOne(optional = false)
    @JoinColumn(name = "match_id")
    private Match match;

    @ManyToOne(optional = false)
    @JoinColumn(name = "season_id")
    private Season season;

    // This particular match's head-to-head
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
    private boolean batsmanDismissed = false;

    @Enumerated(EnumType.STRING)
    private DismissalType dismissalType;
}
