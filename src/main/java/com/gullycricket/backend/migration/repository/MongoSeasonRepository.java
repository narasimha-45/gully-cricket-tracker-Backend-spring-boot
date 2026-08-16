package com.gullycricket.backend.migration.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.gullycricket.backend.migration.documents.MongoSeason;

public interface MongoSeasonRepository extends MongoRepository<MongoSeason, String> {

}
