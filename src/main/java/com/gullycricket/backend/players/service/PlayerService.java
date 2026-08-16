package com.gullycricket.backend.players.service;

import com.gullycricket.backend.common.util.NameNormalizer;
import com.gullycricket.backend.players.dto.PlayerSearchSuggestionDto;
import com.gullycricket.backend.players.entity.Player;
import com.gullycricket.backend.players.repository.PlayerRepository;
import com.gullycricket.backend.search.repository.SearchReadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final SearchReadRepository searchReadRepository;

    public List<PlayerSearchSuggestionDto> searchPlayers(String query) {
        String trimmedQuery = NameNormalizer.normalize(query);
        trimmedQuery = trimmedQuery == null ? "" : trimmedQuery;
        if (trimmedQuery.length() < 2) {
            return List.of();
        }
        log.debug("Searching players with query: {}", trimmedQuery);
        return searchReadRepository.searchPlayers(trimmedQuery);
    }

    @Transactional(readOnly = true)
    public Player getPlayerByName(String name) {
        return playerRepository.findByName(NameNormalizer.normalize(name));
    }

    @Transactional(readOnly = true)
    public List<Player> getAllPlayers() {
        return playerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Player> getPlayersByNameIn(List<String> names) {
        List<String> normalizedNames = names.stream().map(NameNormalizer::normalize).toList();
        return playerRepository.findByNameIn(normalizedNames);
    }

    // Normalizing again here (on top of MatchService's normalization of the incoming
    // match JSON) is a no-op for names that are already normalized, and a safety net
    // for any other write path that creates/updates a Player directly.
    public Player savePlayer(Player player) {
        player.setName(NameNormalizer.normalize(player.getName()));
        return playerRepository.save(player);
    }

    public List<Player> saveAllPlayers(List<Player> players) {
        players.forEach(p -> p.setName(NameNormalizer.normalize(p.getName())));
        return playerRepository.saveAll(players);
    }
}
