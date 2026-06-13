package com.gullycricket.backend.seasons.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "seasons")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Season {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String seasonName;

    @Column(nullable = false)
    private Integer matchesPlayed = 0;

}
