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
        name = "match_player_participation",
        uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "match_id", "team_represented_id"}),
        indexes = {
                @Index(name = "idx_match_participation_player", columnList = "player_id"),
                @Index(name = "idx_match_participation_season_player", columnList = "season_id,player_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MatchPlayerParticipation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private Match match;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id")
    private Season season;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "team_represented_id")
    private Team teamRepresented;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "opposition_team_id")
    private Team oppositionTeam;

    @Enumerated(EnumType.STRING)
    private MatchType matchType;

    @Column(nullable = false)
    private boolean matchWon = false;

    @Column(nullable = false)
    private boolean playerOfTheMatch = false;
}
