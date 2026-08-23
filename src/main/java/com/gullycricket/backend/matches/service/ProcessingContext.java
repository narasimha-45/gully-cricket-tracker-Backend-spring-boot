package com.gullycricket.backend.matches.service;

import com.gullycricket.backend.matches.dto.RulesDto;
import com.gullycricket.backend.matches.entity.Match;
import com.gullycricket.backend.matches.entity.MatchType;
import com.gullycricket.backend.players.entity.Player;
import com.gullycricket.backend.players.entity.PlayerMatch;
import com.gullycricket.backend.players.entity.MatchPlayerParticipation;
import com.gullycricket.backend.players.entity.PlayerPartnerships;
import com.gullycricket.backend.players.entity.PlayerRivalry;
import com.gullycricket.backend.seasons.entity.Season;
import com.gullycricket.backend.teams.entity.PlayerTeam;
import com.gullycricket.backend.teams.entity.Team;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record ProcessingContext(
        Match match,
        Season season,
        MatchType matchType,
        String playerOfTheMatch,
        RulesDto rules,
        Map<String, Player> playerMap,
        Map<String, Team> teamMap,
        Set<String> seasonPlayerIds,
        Map<String, PlayerMatch> playerMatchMap,
        Map<String, PlayerPartnerships> partnershipMap,
        Map<String, PlayerRivalry> rivalryMap,
        Map<String, MatchPlayerParticipation> participationMap,
        List<PlayerTeam> playerTeams
) {}
