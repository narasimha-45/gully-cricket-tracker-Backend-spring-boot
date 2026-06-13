package com.gullycricket.backend.matches.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.gullycricket.backend.seasons.entity.Season;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

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

    @ManyToOne
    @JoinColumn(name = "season_id")
    private Season season;

    @Enumerated(EnumType.STRING)
    private MatchStatus status;

    private String teamA;
    private String teamB;
    private Integer teamAScore;
    private Integer teamAWickets;
    private Integer teamBScore;
    private Integer teamBWickets;
    private String winner;
    private Boolean superOver;
    private String wonBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode matchData;

    private LocalDateTime completedAt;

}

