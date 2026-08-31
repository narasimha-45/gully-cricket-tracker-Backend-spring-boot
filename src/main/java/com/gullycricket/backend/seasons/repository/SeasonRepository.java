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


    @Modifying(flushAutomatically = true)
    @Query(value = """
            UPDATE seasons s
            SET matches_played = (
                SELECT COUNT(*) FROM matches m WHERE m.season_id = s.id
            )
            WHERE s.id = :seasonId
            """, nativeQuery = true)
    int syncMatchesPlayed(@Param("seasonId") String seasonId);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            UPDATE seasons s
            SET matches_played = (
                SELECT COUNT(*) FROM matches m WHERE m.season_id = s.id
            )
            """, nativeQuery = true)
    int syncAllMatchesPlayed();
}

