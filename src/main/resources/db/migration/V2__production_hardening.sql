-- Upgrade deployments whose original schema was created by Hibernate before Flyway.
ALTER TABLE matches ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(128);
ALTER TABLE matches ADD COLUMN IF NOT EXISTS version BIGINT;
UPDATE matches SET version = 0 WHERE version IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_matches_idempotency_key
    ON matches(idempotency_key)
    WHERE idempotency_key IS NOT NULL;

ALTER TABLE match_innings_summary ADD COLUMN IF NOT EXISTS sequence_number INTEGER;
WITH numbered AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY match_id
               ORDER BY team_innings_number, batting_team_id, id
           ) AS seq
    FROM match_innings_summary
    WHERE sequence_number IS NULL
)
UPDATE match_innings_summary m
SET sequence_number = numbered.seq
FROM numbered
WHERE m.id = numbered.id;

ALTER TABLE match_innings_summary ALTER COLUMN sequence_number SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_match_innings_sequence
    ON match_innings_summary(match_id, sequence_number);
CREATE INDEX IF NOT EXISTS idx_innings_match_id ON match_innings_summary(match_id);
CREATE INDEX IF NOT EXISTS idx_innings_batting_team_id ON match_innings_summary(batting_team_id);
CREATE INDEX IF NOT EXISTS idx_innings_bowling_team_id ON match_innings_summary(bowling_team_id);
CREATE INDEX IF NOT EXISTS idx_matches_season_status_completed ON matches(season_id, status, completed_at);
CREATE INDEX IF NOT EXISTS idx_player_matches_season_player ON player_matches(season_id, player_id);
CREATE INDEX IF NOT EXISTS idx_player_matches_match_player ON player_matches(match_id, player_id);
CREATE INDEX IF NOT EXISTS idx_partnerships_season_team ON player_partnerships(season_id, team_represented_id);
CREATE INDEX IF NOT EXISTS idx_rivalries_season_batter_bowler ON player_rivalries(season_id, batsman_id, bowler_id);

-- Detect duplicate canonical team names before the application starts relying on uniqueness.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM teams GROUP BY team_name HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Duplicate team_name values exist. Merge duplicates before applying the unique team-name constraint.';
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_teams_team_name ON teams(team_name);
