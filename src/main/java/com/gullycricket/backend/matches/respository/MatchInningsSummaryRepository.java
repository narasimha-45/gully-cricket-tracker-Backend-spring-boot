package com.gullycricket.backend.matches.respository;

import com.gullycricket.backend.matches.entity.MatchInningsSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchInningsSummaryRepository extends JpaRepository<MatchInningsSummary, String> {
}
