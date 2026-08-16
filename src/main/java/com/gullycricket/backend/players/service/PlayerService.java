package com.gullycricket.backend.players.service;

import com.gullycricket.backend.common.util.NameNormalizer;
import com.gullycricket.backend.players.DTOs.PlayerSearchSuggestionDto;
import com.gullycricket.backend.players.entity.Player;
import com.gullycricket.backend.players.repository.PlayerMatchRepository;
import com.gullycricket.backend.players.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final PlayerMatchRepository playerMatchRepository;

    @Transactional(readOnly = true)
    public List<PlayerSearchSuggestionDto> searchPlayers(String query) {
        String trimmedQuery = query == null ? "" : query.trim();
        log.info("Searching players with query: {}", trimmedQuery);

        List<Player> players = playerRepository.findByNameContainingIgnoreCase(trimmedQuery);

        // ONE grouped query for match counts across every player in the result set,
        // instead of lazily loading each player's playerMatches collection one at a
        // time (which used to be an N+1 query per search request).
        List<String> playerIds = players.stream().map(Player::getId).toList();
        Map<String, Long> matchCountsByPlayerId = playerIds.isEmpty()
                ? Map.of()
                : playerMatchRepository.countMatchesByPlayerIds(playerIds).stream()
                        .collect(Collectors.toMap(row -> (String) row[0], row -> (Long) row[1]));

        List<PlayerSearchSuggestionDto> response = players.stream()
                .map(p -> new PlayerSearchSuggestionDto(
                        p.getId(),
                        p.getName(),
                        matchCountsByPlayerId.getOrDefault(p.getId(), 0L).intValue()
                ))
                .toList();

        log.info("Response of players: {}", response);

        return response;
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
