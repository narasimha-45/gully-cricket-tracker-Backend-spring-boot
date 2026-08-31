-- Companion to V8: V8 added per-innings is_follow_on/completion_reason, but the
-- match-level testConfig (inningsPerTeam, followOnEnforced) sent by the frontend
-- had nowhere to land at all — MatchDataDto didn't declare the field, so it was
-- silently dropped before it ever reached matches.match_data. Existing rows
-- default to NULL/false since that history was never captured.

ALTER TABLE matches
    ADD COLUMN IF NOT EXISTS test_innings_per_team SMALLINT;

ALTER TABLE matches
    ADD COLUMN IF NOT EXISTS follow_on_enforced BOOLEAN NOT NULL DEFAULT FALSE;
