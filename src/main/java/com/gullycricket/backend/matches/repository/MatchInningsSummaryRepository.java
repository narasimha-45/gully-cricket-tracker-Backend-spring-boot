package com.gullycricket.backend.matches.repository;

import com.gullycricket.backend.matches.entity.MatchInningsSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchInningsSummaryRepository extends JpaRepository<MatchInningsSummary, String> {
    List<MatchInningsSummary> findByMatch_IdOrderBySequenceNumber(String matchId);
}
