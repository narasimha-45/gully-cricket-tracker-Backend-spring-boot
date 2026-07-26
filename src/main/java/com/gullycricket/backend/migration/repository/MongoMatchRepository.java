package com.gullycricket.backend.migration.repository;

import com.gullycricket.backend.migration.documents.MongoMatch;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoMatchRepository extends MongoRepository<MongoMatch, String> {

}
