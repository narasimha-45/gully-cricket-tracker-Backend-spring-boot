-- Marks partnerships that ended because a batter was run out, so run-out
-- involvement can be queried per player and per pair without scanning match_data.
--
-- Backfill: partnership rows already store who_got_out_id, and player_matches
-- already stores how each batter was dismissed (dismissal_type) per innings.
-- Joining the two recovers the flag for every existing partnership row without
-- replaying any match.
--
-- Legacy matches saved without ball-by-ball data never produced partnership
-- rows, so there is nothing to flag for them; that history cannot be recovered
-- (who was at the crease with whom is not stored anywhere).

ALTER TABLE player_partnerships
    ADD COLUMN IF NOT EXISTS broken_by_run_out BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE player_partnerships pp
SET broken_by_run_out = TRUE
FROM player_matches pm
WHERE pp.partnership_broken = TRUE
  AND pp.who_got_out_id IS NOT NULL
  AND pm.match_id = pp.match_id
  AND pm.player_id = pp.who_got_out_id
  AND pm.innings_number = pp.innings_number
  AND pm.out = TRUE
  AND pm.dismissal_type = 'RUN_OUT';

CREATE INDEX IF NOT EXISTS idx_partnerships_run_out_p1
    ON player_partnerships(player1_id) WHERE broken_by_run_out;
CREATE INDEX IF NOT EXISTS idx_partnerships_run_out_p2
    ON player_partnerships(player2_id) WHERE broken_by_run_out;
