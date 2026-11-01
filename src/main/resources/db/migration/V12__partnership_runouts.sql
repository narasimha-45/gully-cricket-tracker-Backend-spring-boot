-- Nullable to distinguish legacy unknown data from a confirmed non-run-out.
ALTER TABLE player_partnerships ADD COLUMN run_out BOOLEAN;
CREATE INDEX idx_partnership_run_out_players ON player_partnerships (player1_id, player2_id) WHERE run_out = TRUE;

-- Recover dismissal type from archived innings summaries, even when balls are absent.
-- Do not classify unmatched legacy rows as FALSE: their status remains unknown.
WITH archived AS (
    SELECT pp.id AS partnership_id,
           upper(replace(innings.data -> 'dismissals' -> p.name ->> 'type', ' ', '_')) AS dismissal_type
    FROM player_partnerships pp
    JOIN matches m ON m.id = pp.match_id
    JOIN players p ON p.id = pp.who_got_out_id
    CROSS JOIN LATERAL jsonb_array_elements(
        CASE WHEN jsonb_typeof(m.match_data -> 'innings') = 'array'
             THEN m.match_data -> 'innings' ELSE '[]'::jsonb END
    ) WITH ORDINALITY AS innings(data, position)
    WHERE pp.partnership_broken = TRUE
      AND pp.innings_number = innings.position
      AND jsonb_typeof(innings.data -> 'dismissals') = 'object'
)
UPDATE player_partnerships pp
SET run_out = (archived.dismissal_type = 'RUN_OUT')
FROM archived
WHERE pp.id = archived.partnership_id
  AND archived.dismissal_type IS NOT NULL;

-- Ball history can fill in cases where the innings summary was not retained.
WITH archived_balls AS (
    SELECT pp.id AS partnership_id,
           upper(replace(ball.data -> 'wicket' ->> 'type', ' ', '_')) AS dismissal_type
    FROM player_partnerships pp
    JOIN matches m ON m.id = pp.match_id
    JOIN players p ON p.id = pp.who_got_out_id
    CROSS JOIN LATERAL jsonb_array_elements(
        CASE WHEN jsonb_typeof(m.match_data -> 'innings') = 'array'
             THEN m.match_data -> 'innings' ELSE '[]'::jsonb END
    ) WITH ORDINALITY AS innings(data, position)
    CROSS JOIN LATERAL jsonb_array_elements(
        CASE WHEN jsonb_typeof(innings.data -> 'ballByBall') = 'array'
             THEN innings.data -> 'ballByBall' ELSE '[]'::jsonb END
    ) AS ball(data)
    WHERE pp.partnership_broken = TRUE
      AND pp.innings_number = innings.position
      AND ball.data -> 'wicket' ->> 'outBatsman' = p.name
      AND pp.run_out IS NULL
)
UPDATE player_partnerships pp
SET run_out = (archived_balls.dismissal_type = 'RUN_OUT')
FROM archived_balls
WHERE pp.id = archived_balls.partnership_id
  AND archived_balls.dismissal_type IS NOT NULL;
