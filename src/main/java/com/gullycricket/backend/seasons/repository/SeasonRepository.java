package com.gullycricket.backend.seasons.repository;

import com.gullycricket.backend.seasons.entity.Season;
import org.springframework.data.jpa.repository.JpaRepository;


public interface SeasonRepository extends JpaRepository<Season, String> {
}
