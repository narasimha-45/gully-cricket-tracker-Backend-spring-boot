CREATE TABLE IF NOT EXISTS match_player_participation (
    id VARCHAR(255) PRIMARY KEY,
    player_id VARCHAR(255) NOT NULL REFERENCES players(id),
    match_id VARCHAR(255) NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    season_id VARCHAR(255) NOT NULL REFERENCES seasons(id),
    team_represented_id VARCHAR(255) NOT NULL REFERENCES teams(id),
    opposition_team_id VARCHAR(255) NOT NULL REFERENCES teams(id),
    match_type VARCHAR(255),
    match_won BOOLEAN NOT NULL DEFAULT FALSE,
    player_of_the_match BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE(player_id, match_id, team_represented_id)
);

CREATE INDEX IF NOT EXISTS idx_match_participation_player
    ON match_player_participation(player_id);
CREATE INDEX IF NOT EXISTS idx_match_participation_season_player
    ON match_player_participation(season_id, player_id);

-- Backfill all historical participants already represented by the per-innings
-- projection. Future matches populate this table from the full match squad,
-- including players who neither bat nor bowl.
INSERT INTO match_player_participation (
    id, player_id, match_id, season_id, team_represented_id, opposition_team_id,
    match_type, match_won, player_of_the_match
)
SELECT
    md5(pm.player_id || ':' || pm.match_id || ':' || pm.team_represented_id),
    pm.player_id,
    pm.match_id,
    MAX(pm.season_id),
    MAX(pm.team_represented_id),
    MAX(pm.opposition_team_id),
    MAX(pm.match_type),
    BOOL_OR(pm.match_won),
    BOOL_OR(pm.player_of_the_match)
FROM player_matches pm
GROUP BY pm.player_id, pm.match_id, pm.team_represented_id
ON CONFLICT (player_id, match_id, team_represented_id) DO NOTHING;
