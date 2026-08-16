package com.gullycricket.backend.teams.entity;

import com.gullycricket.backend.seasons.entity.Season;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "teams", uniqueConstraints = @UniqueConstraint(name = "uk_teams_team_name", columnNames = "team_name"))
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "team_name", nullable = false, unique = true, length = 120)
    private String teamName;

    @ManyToMany
    @JoinTable(
            name = "team_season",
            joinColumns = @JoinColumn(name = "team_id"),
            inverseJoinColumns = @JoinColumn(name = "season_id")
    )
    private Set<Season> seasonsPlayed = new HashSet<>();

}
