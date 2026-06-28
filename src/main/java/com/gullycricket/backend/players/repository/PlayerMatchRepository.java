package com.gullycricket.backend.players.repository;

import com.gullycricket.backend.players.entity.PlayerMatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerMatchRepository extends JpaRepository<PlayerMatch,String> {
}
