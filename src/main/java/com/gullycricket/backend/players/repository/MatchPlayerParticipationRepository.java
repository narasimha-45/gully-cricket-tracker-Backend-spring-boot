package com.gullycricket.backend.players.repository;

import com.gullycricket.backend.players.entity.MatchPlayerParticipation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchPlayerParticipationRepository extends JpaRepository<MatchPlayerParticipation, String> {
}
