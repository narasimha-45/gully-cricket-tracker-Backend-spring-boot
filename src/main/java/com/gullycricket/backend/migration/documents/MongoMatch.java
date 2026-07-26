package com.gullycricket.backend.migration.documents;

import com.gullycricket.backend.migration.DTOs.MongoInningsDTO;
import com.gullycricket.backend.migration.DTOs.MongoResultDTO;
import com.gullycricket.backend.migration.DTOs.MongoRulesDTO;
import com.gullycricket.backend.migration.DTOs.MongoTeamsDTO;
import com.gullycricket.backend.migration.DTOs.MongoTossDTO;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "matches")
public class MongoMatch {

    @Id
    private String id;

    private String seasonId;

    private String matchType;

    private MongoTeamsDTO teams;

    private MongoTossDTO toss;

    private MongoRulesDTO rules;

    private Integer totalOvers;

    private List<MongoInningsDTO> innings;

    private MongoResultDTO result;

    private String status;

    private LocalDateTime completedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}