package com.gullycricket.backend.teams.reposistory;

import com.gullycricket.backend.players.entity.Player;
import com.gullycricket.backend.seasons.entity.Season;
import com.gullycricket.backend.teams.entity.PlayerTeam;
import com.gullycricket.backend.teams.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerTeamRepository extends JpaRepository<PlayerTeam,String> {
    boolean existsByPlayerAndTeamAndSeason(
            Player player,
            Team team,
            Season season
    );
}
