-- Backfill match participation from the authoritative squad lists stored in matches.match_data.
-- A player counts as having played a match when the player is listed in teamA/teamB,
-- even when that player did not bat or bowl and therefore has no player_matches row.
--
-- This migration is idempotent because match_player_participation has a unique key on
-- (player_id, match_id, team_represented_id) and conflicts are ignored.

WITH squad_players AS (
    SELECT
        m.id AS match_id,
        m.season_id,
        m.match_type,
        m.winner_team_id,
        m.match_data,
        m.team_a_id AS team_represented_id,
        m.team_b_id AS opposition_team_id,
        squad.player_name
    FROM matches m
    CROSS JOIN LATERAL jsonb_array_elements_text(
        COALESCE(m.match_data -> 'teams' -> 'teamA' -> 'players', '[]'::jsonb)
    ) AS squad(player_name)
    WHERE m.status = 'COMPLETED'

    UNION ALL

    SELECT
        m.id AS match_id,
        m.season_id,
        m.match_type,
        m.winner_team_id,
        m.match_data,
        m.team_b_id AS team_represented_id,
        m.team_a_id AS opposition_team_id,
        squad.player_name
    FROM matches m
    CROSS JOIN LATERAL jsonb_array_elements_text(
        COALESCE(m.match_data -> 'teams' -> 'teamB' -> 'players', '[]'::jsonb)
    ) AS squad(player_name)
    WHERE m.status = 'COMPLETED'
),
resolved_players AS (
    SELECT
        sp.*,
        p.id AS player_id,
        p.name AS canonical_player_name
    FROM squad_players sp
    JOIN players p
      ON regexp_replace(lower(trim(p.name)), '\s+', ' ', 'g') =
         regexp_replace(lower(trim(sp.player_name)), '\s+', ' ', 'g')
)
INSERT INTO match_player_participation (
    id,
    player_id,
    match_id,
    season_id,
    team_represented_id,
    opposition_team_id,
    match_type,
    match_won,
    player_of_the_match
)
SELECT
    md5(r.player_id || ':' || r.match_id || ':' || r.team_represented_id) AS id,
    r.player_id,
    r.match_id,
    r.season_id,
    r.team_represented_id,
    r.opposition_team_id,
    r.match_type,
    COALESCE(r.winner_team_id = r.team_represented_id, FALSE) AS match_won,
    COALESCE(
        regexp_replace(lower(trim(r.match_data -> 'result' ->> 'manOfTheMatch')), '\s+', ' ', 'g') =
        regexp_replace(lower(trim(r.canonical_player_name)), '\s+', ' ', 'g'),
        FALSE
    ) AS player_of_the_match
FROM resolved_players r
ON CONFLICT (player_id, match_id, team_represented_id) DO NOTHING;
