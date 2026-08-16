package com.gullycricket.backend.teams.entity;

import com.gullycricket.backend.players.entity.Player;
import com.gullycricket.backend.seasons.entity.Season;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "player_teams",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"player_id", "team_id", "season_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlayerTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id")
    private Season season;

    @Column(nullable = false)
    private boolean active = true;
}
