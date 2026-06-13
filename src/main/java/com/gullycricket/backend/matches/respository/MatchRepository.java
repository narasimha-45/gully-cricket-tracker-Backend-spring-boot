package com.gullycricket.backend.matches.respository;

import com.gullycricket.backend.matches.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MatchRepository extends JpaRepository<Match, String> {
    List<Match> findBySeason_Id(String seasonId);
}
