package com.gullycricket.backend.players.service;

import com.gullycricket.backend.players.DTOs.PlayerSearchSuggestionDto;
import com.gullycricket.backend.players.entity.Player;
import com.gullycricket.backend.players.repository.PlayerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Slf4j
@Service
public class PlayerSearchService {

    @Autowired
    private PlayerRepository playerRepository;


    public List<PlayerSearchSuggestionDto> searchPlayers(String query) {
        log.info("Searching players with query: {}", query);

        List<PlayerSearchSuggestionDto> players = playerRepository.findByNameContainingIgnoreCase(query)
                .stream()
                .map(this::mapToPlayerSearchSuggestionDto)
                .toList();

        log.info("Response of players: {}",players);

        return players;
    }

    private PlayerSearchSuggestionDto mapToPlayerSearchSuggestionDto(Player player){
        return new PlayerSearchSuggestionDto(
                player.getId(), player.getName(),player.getPlayerMatches().size()
        );
    }
}
