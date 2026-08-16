package com.gullycricket.backend.seasons.repository;

import com.gullycricket.backend.seasons.entity.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SeasonRepository extends JpaRepository<Season, String> {

    List<Season> findTop10BySeasonNameContainingIgnoreCaseOrderBySeasonNameAsc(String name);

    Optional<Season> findFirstBySeasonNameIgnoreCase(String seasonName);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Season s SET s.matchesPlayed = s.matchesPlayed + 1 WHERE s.id = :seasonId")
    int incrementMatchesPlayed(@Param("seasonId") String seasonId);
}
