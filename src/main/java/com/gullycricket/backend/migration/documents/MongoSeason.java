package com.gullycricket.backend.migration.documents;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "seasons")
public class MongoSeason {

    @Id
    private String id;

    private String seasonName;

    private List<String> matches;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}