package com.gullycricket.backend.seasons.entity;

import com.gullycricket.backend.players.entity.Player;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(
        name = "season_players",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"player_id", "season_id"})
        }
)
public class SeasonPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "player_id")
    private Player player;

    @ManyToOne
    @JoinColumn(name = "season_id")
    private Season season;
}
