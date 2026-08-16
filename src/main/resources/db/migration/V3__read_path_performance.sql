-- Read-path indexes for leaderboards, rosters and season/team navigation.
-- These are deliberately narrow/partial so match ingestion remains inexpensive.

CREATE INDEX IF NOT EXISTS idx_pm_batting_season_player
    ON player_matches(season_id, player_id)
    WHERE batted = TRUE;

CREATE INDEX IF NOT EXISTS idx_pm_bowling_season_player
    ON player_matches(season_id, player_id)
    WHERE bowled = TRUE;

CREATE INDEX IF NOT EXISTS idx_pm_batting_team_player
    ON player_matches(team_represented_id, player_id)
    WHERE batted = TRUE;

CREATE INDEX IF NOT EXISTS idx_pm_bowling_team_player
    ON player_matches(team_represented_id, player_id)
    WHERE bowled = TRUE;

CREATE INDEX IF NOT EXISTS idx_pm_batting_opponent_player
    ON player_matches(opposition_team_id, player_id)
    WHERE batted = TRUE;

CREATE INDEX IF NOT EXISTS idx_pm_bowling_opponent_player
    ON player_matches(opposition_team_id, player_id)
    WHERE bowled = TRUE;

CREATE INDEX IF NOT EXISTS idx_pm_batting_position
    ON player_matches(batting_position, player_id)
    WHERE batted = TRUE AND batting_position IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_team_season_season_team
    ON team_season(season_id, team_id);

CREATE INDEX IF NOT EXISTS idx_player_teams_team_season_active
    ON player_teams(team_id, season_id, active, player_id);

CREATE INDEX IF NOT EXISTS idx_matches_completed_at_desc
    ON matches(completed_at DESC)
    WHERE status = 'COMPLETED';

CREATE INDEX IF NOT EXISTS idx_matches_completed_season_date
    ON matches(season_id, completed_at DESC)
    WHERE status = 'COMPLETED';
