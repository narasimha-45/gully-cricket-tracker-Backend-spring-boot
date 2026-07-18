package com.gullycricket.backend.players.service;

import com.gullycricket.backend.players.DTOs.PlayerSearchSuggestionDto;
import com.gullycricket.backend.players.entity.Player;
import com.gullycricket.backend.players.repository.PlayerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class PlayerService {

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

    public Player getPlayerByName(String name){
        return playerRepository.findByName(name);
    }

    private PlayerSearchSuggestionDto mapToPlayerSearchSuggestionDto(Player player){
        return new PlayerSearchSuggestionDto(
                player.getId(), player.getName(),player.getPlayerMatches().size()
        );
    }

    public Player savePlayer(Player player){
        return playerRepository.save(player);
    }

    public List<Player> getAllPlayers(){
        return playerRepository.findAll();
    }

    public List<Player> getPlayersByNameIn(List<String> names) {
        return playerRepository.findByNameIn(names);
    }

    public List<Player> saveAllPlayers(List<Player> players){
        return playerRepository.saveAll(players);
    }
}
