package com.gullycricket.backend.seasons.repository;

import com.gullycricket.backend.seasons.entity.Season;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface SeasonRepository extends JpaRepository<Season, String> {

    List<Season> findBySeasonNameContainingIgnoreCase(String name);
}
