package com.gullycricket.backend.teams.reposistory;

import com.gullycricket.backend.teams.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TeamRepository extends JpaRepository<Team,String> {

    List<Team> findByTeamNameContainingIgnoreCase(String name);

    Team findByTeamName(String name);
}
