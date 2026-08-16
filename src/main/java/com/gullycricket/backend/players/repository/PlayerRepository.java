package com.gullycricket.backend.players.repository;

import com.gullycricket.backend.players.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerRepository extends JpaRepository<Player, String> {
    List<Player> findTop10ByNameContainingIgnoreCaseOrderByNameAsc(String name);
    Player findByName(String name);
    List<Player> findByNameIn(List<String> names);
}
