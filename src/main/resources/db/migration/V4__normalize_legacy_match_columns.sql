-- Normalize legacy match columns created by older Hibernate naming rules.
--
-- Older deployments created directly by Hibernate can contain columns such as
-- "teamascore" for the Java field "teamAScore". The optimized JDBC read model
-- intentionally uses explicit snake_case SQL names, so those deployments must be
-- upgraded once. New installations created from V1 already use the canonical
-- names and this migration becomes a no-op.

DO $$
DECLARE
    column_mapping RECORD;
    old_exists BOOLEAN;
    new_exists BOOLEAN;
BEGIN
    FOR column_mapping IN
        SELECT * FROM (VALUES
            ('teamascore',        'team_a_score'),
            ('teamawickets',      'team_a_wickets'),
            ('teamaballs_faced',  'team_a_balls_faced'),
            ('teambscore',        'team_b_score'),
            ('teambwickets',      'team_b_wickets'),
            ('teambballs_faced',  'team_b_balls_faced')
        ) AS mapping(old_name, new_name)
    LOOP
        SELECT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = current_schema()
              AND table_name = 'matches'
              AND column_name = column_mapping.old_name
        ) INTO old_exists;

        SELECT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = current_schema()
              AND table_name = 'matches'
              AND column_name = column_mapping.new_name
        ) INTO new_exists;

        IF old_exists AND NOT new_exists THEN
            EXECUTE format(
                'ALTER TABLE %I.matches RENAME COLUMN %I TO %I',
                current_schema(),
                column_mapping.old_name,
                column_mapping.new_name
            );
        ELSIF old_exists AND new_exists THEN
            RAISE EXCEPTION
                'Legacy and canonical columns both exist on matches: % and %. Reconcile them before continuing to avoid data loss.',
                column_mapping.old_name,
                column_mapping.new_name;
        END IF;
    END LOOP;
END $$;

-- Fail fast with a clear migration error if an unexpected legacy schema is still
-- present. This is much easier to diagnose than a JDBC BadSqlGrammarException at runtime.
DO $$
DECLARE
    required_column TEXT;
BEGIN
    FOREACH required_column IN ARRAY ARRAY[
        'team_a_score',
        'team_a_wickets',
        'team_a_balls_faced',
        'team_b_score',
        'team_b_wickets',
        'team_b_balls_faced'
    ]
    LOOP
        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = current_schema()
              AND table_name = 'matches'
              AND column_name = required_column
        ) THEN
            RAISE EXCEPTION
                'Required canonical column matches.% is missing after legacy schema normalization.',
                required_column;
        END IF;
    END LOOP;
END $$;

-- Reassert the invariants expected by both JPA and the JDBC read model.
UPDATE matches SET team_a_score = 0 WHERE team_a_score IS NULL;
UPDATE matches SET team_a_wickets = 0 WHERE team_a_wickets IS NULL;
UPDATE matches SET team_a_balls_faced = 0 WHERE team_a_balls_faced IS NULL;
UPDATE matches SET team_b_score = 0 WHERE team_b_score IS NULL;
UPDATE matches SET team_b_wickets = 0 WHERE team_b_wickets IS NULL;
UPDATE matches SET team_b_balls_faced = 0 WHERE team_b_balls_faced IS NULL;

ALTER TABLE matches ALTER COLUMN team_a_score SET DEFAULT 0;
ALTER TABLE matches ALTER COLUMN team_a_score SET NOT NULL;
ALTER TABLE matches ALTER COLUMN team_a_wickets SET DEFAULT 0;
ALTER TABLE matches ALTER COLUMN team_a_wickets SET NOT NULL;
ALTER TABLE matches ALTER COLUMN team_a_balls_faced SET DEFAULT 0;
ALTER TABLE matches ALTER COLUMN team_a_balls_faced SET NOT NULL;
ALTER TABLE matches ALTER COLUMN team_b_score SET DEFAULT 0;
ALTER TABLE matches ALTER COLUMN team_b_score SET NOT NULL;
ALTER TABLE matches ALTER COLUMN team_b_wickets SET DEFAULT 0;
ALTER TABLE matches ALTER COLUMN team_b_wickets SET NOT NULL;
ALTER TABLE matches ALTER COLUMN team_b_balls_faced SET DEFAULT 0;
ALTER TABLE matches ALTER COLUMN team_b_balls_faced SET NOT NULL;
